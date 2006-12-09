/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.codehaus.wadi.impl;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.Relocater;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.EndPoint;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.location.PartitionManager;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.MoveIMToSM;
import org.codehaus.wadi.location.session.MovePMToIM;
import org.codehaus.wadi.location.session.MovePMToIMInvocation;
import org.codehaus.wadi.location.session.MoveSMToIM;
import org.codehaus.wadi.servicespace.ServiceSpace;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Combine various RelocationStrategies to produce a cleverer one
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class HybridRelocater implements Relocater {
	private static final Log log = LogFactory.getLog(HybridRelocater.class);

	private final Dispatcher dispatcher;
    private final LocalPeer localPeer;
	private final PartitionManager partitionManager;
    private final long resTimeout;

    public HybridRelocater(ServiceSpace serviceSpace,
            PartitionManager partitionManager,
            long resTimeout) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == partitionManager) {
            throw new IllegalArgumentException("partitionManager is required");
        } else if (1 > resTimeout) { 
            throw new IllegalArgumentException("resTimeout must be > 0");
        }
        this.partitionManager = partitionManager;
        this.resTimeout = resTimeout;

        dispatcher = serviceSpace.getDispatcher();
        localPeer = dispatcher.getCluster().getLocalPeer();
    }

    public boolean relocate(Invocation invocation, String name, Immoter immoter, Sync motionLock, boolean shuttingDown) throws InvocationException {
        try {
            Motable immotable = doRelocate(invocation, name, shuttingDown, resTimeout, immoter);
            if (null == immotable) {
                return false;
            }
            return immoter.contextualise(invocation, name, immotable, motionLock);
        } catch (Exception e) {
            log.error("unexpected error", e);
            return false;
        }
    }
    
    protected Motable doRelocate(Invocation invocation, String sessionName, boolean shuttingDown, long timeout, Immoter immoter) throws Exception {
        MoveIMToPM request = new MoveIMToPM(localPeer, sessionName, !shuttingDown, invocation.getRelocatable());
        Envelope message = partitionManager.getPartition(sessionName).exchange(request, timeout);

        if (message == null) {
            log.error("Something went wrong");
            return null;
        }

        Serializable dm = message.getPayload();
        if (dm instanceof MoveSMToIM) {
            // We are receiving an incoming state migration...
            MoveSMToIM req = (MoveSMToIM) dm;
            // insert motable into contextualiser stack...
            Motable emotable = req.getMotable();
            if (emotable == null) {
                log.warn("failed relocation - 0 bytes arrived: " + sessionName);
                return null;
            } else {
                Emoter emoter = new SMToIMEmoter(message);
                immoter = new SMToIMImmoter(immoter, emotable);
                Motable immotable = Utils.mote(emoter, immoter, emotable, sessionName);
                return immotable;
            }
        } else if (dm instanceof MovePMToIM) {
            // The Partition manager had no record of our session key - either the session
            // has already been destroyed, or never existed...
            log.info("Unknown session [" + sessionName + "]");
            return null;
        } else if (dm instanceof MovePMToIMInvocation) {
            // we are going to relocate our Invocation to the SM...
            Peer smPeer = ((MovePMToIMInvocation) dm).getStateMaster();
            EndPoint endPoint=smPeer.getPeerInfo().getEndPoint();
            invocation.relocate(endPoint);
            return null;
        } else {
            throw new WADIRuntimeException("unexpected response returned - what should I do? : " + dm);
        }
    }

    class SMToIMImmoter implements Immoter {
        protected final Log _log = LogFactory.getLog(SMToIMImmoter.class);
        private final Immoter delegate;
        private final Motable emotable;
        
        public SMToIMImmoter(Immoter delegate, Motable emotable) {
            this.delegate = delegate;
            this.emotable = emotable;
        }

        public boolean contextualise(Invocation invocation, String id, Motable immotable, Sync motionLock) throws InvocationException {
            return delegate.contextualise(invocation, id, immotable, motionLock);
        }

        public boolean immote(Motable emotable, Motable immotable) {
            return delegate.immote(emotable, immotable);
        }

        public Motable newMotable() {
            Motable immotable = delegate.newMotable();
            try {
                immotable.rehydrate(emotable.getCreationTime(),
                        emotable.getLastAccessedTime(),
                        emotable.getMaxInactiveInterval(),
                        emotable.getName(),
                        emotable.getBodyAsByteArray());
            } catch (Exception e) {
                throw new WADIRuntimeException(e);
            }
            return immotable;
        }
        
    }
    
    class SMToIMEmoter extends AbstractChainedEmoter {
        protected final Log _log = LogFactory.getLog(SMToIMEmoter.class);
        protected final Envelope _message;
        protected Sync _invocationLock;
        protected Sync _stateLock;

        public SMToIMEmoter(Envelope message) {
            _message = message;
        }

        public boolean emote(Motable emotable, Motable immotable) {
            try {
                MoveIMToSM response = new MoveIMToSM(true);
                dispatcher.reply(_message, response);
            } catch (Exception e) {
                _log.warn(e);
                return false;
            }
            return true;
        }
        
    }

}
