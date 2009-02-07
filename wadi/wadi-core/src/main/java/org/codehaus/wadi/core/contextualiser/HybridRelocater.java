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
package org.codehaus.wadi.core.contextualiser;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.MotableBusyException;
import org.codehaus.wadi.core.WADIRuntimeException;
import org.codehaus.wadi.core.motable.Emoter;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.LockingRehydrationImmoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.util.Utils;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.EndPoint;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.location.partitionmanager.PartitionManager;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.MoveIMToSM;
import org.codehaus.wadi.location.session.MovePMToIM;
import org.codehaus.wadi.location.session.MovePMToIMInvocation;
import org.codehaus.wadi.location.session.MoveSMToIM;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.servicespace.ServiceSpace;

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
	private final ReplicationManager replicationManager;

    public HybridRelocater(ServiceSpace serviceSpace,
            PartitionManager partitionManager,
            ReplicationManager replicationManager) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == partitionManager) {
            throw new IllegalArgumentException("partitionManager is required");
        } else if (null == replicationManager) {
            throw new IllegalArgumentException("replicationManager is required");
        }
        this.partitionManager = partitionManager;
        this.replicationManager = replicationManager;

        dispatcher = serviceSpace.getDispatcher();
        localPeer = dispatcher.getCluster().getLocalPeer();
    }

    public boolean relocate(Invocation invocation, Object id, Immoter immoter, boolean shuttingDown) throws InvocationException {
        try {
            return doRelocate(invocation, id, immoter, shuttingDown);
        } catch (MotableBusyException e) {
            throw e;
        } catch (Exception e) {
            log.error("unexpected error", e);
            return false;
        }
    }
    
    protected boolean doRelocate(Invocation invocation, Object id, Immoter immoter, boolean shuttingDown) throws Exception {
        long exclusiveSessionLockWaitTime = invocation.getExclusiveSessionLockWaitTime();
        MoveIMToPM request = new MoveIMToPM(localPeer, 
            id,
            !shuttingDown, 
            invocation.isRelocatable(),
            exclusiveSessionLockWaitTime);
        Envelope message;
        try {
            message = partitionManager.getPartition(id).exchange(request, exclusiveSessionLockWaitTime + 10000);
        } catch (MessageExchangeException e) {
            throw new WADIRuntimeException(e);
        }

        if (message == null) {
            throw new WADIRuntimeException("Something went wrong during a session relocation.");
        }

        Serializable dm = message.getPayload();
        if (dm instanceof MoveSMToIM) {
            return handleSessionRelocation(invocation, id, immoter, message, (MoveSMToIM) dm);
        } else if (dm instanceof MovePMToIM) {
            return handleUnknownSession(id);
        } else if (dm instanceof MovePMToIMInvocation) {
            return handleInvocationRelocation(invocation, (MovePMToIMInvocation) dm);
        } else {
            throw new WADIRuntimeException("unexpected response returned [" + dm + "]");
        }
    }

    protected boolean handleInvocationRelocation(Invocation invocation, MovePMToIMInvocation dm) throws InvocationException {
        // we are going to relocate our Invocation to the SM...
        Peer smPeer = dm.getStateMaster();
        EndPoint endPoint = smPeer.getPeerInfo().getEndPoint();
        invocation.relocate(endPoint);
        return true;
    }

    protected boolean handleUnknownSession(Object id) {
        // The Partition manager had no record of our session key - either the session
        // has already been destroyed, or never existed...
        log.debug("Unknown session [" + id + "]");
        return false;
    }

    protected boolean handleSessionRelocation(Invocation invocation, Object id, Immoter immoter, Envelope message, MoveSMToIM req) throws InvocationException {
        Motable emotable = req.getMotable();
        if (null == emotable) {
            log.warn("Failed relocation for [" + id + "]");
            if (req.isSessionBuzy()) {
                throw new MotableBusyException("Motable [" + id + "] buzy. Session relocation has been aborted.");
            }
            return false;
        }

        // We are receiving an incoming state migration. Insert motable into contextualiser stack...
        Emoter emoter = newSessionRelocationEmoter();
        immoter = newSessionRelocationImmoter(invocation, immoter);
        Motable immotable = mote(immoter, emotable, emoter);

        MoveIMToSM response = new MoveIMToSM(true);
        try {
            dispatcher.reply(message, response);
        } catch (MessageExchangeException e) {
            log.warn("Session migration has not been acknowledged. SM has disappeared.", e);
        }
        
        replicationManager.promoteToMaster(id, req.getReplicaInfo(), immotable, null);

        return immoter.contextualise(invocation, id, immotable);
    }

    protected Motable mote(Immoter immoter, Motable emotable, Emoter emoter) {
        return Utils.mote(emoter, immoter, emotable);
    }

    protected Immoter newSessionRelocationImmoter(Invocation invocation, Immoter immoter) {
        MotableLockHandler lockHandler = new BasicMotableLockHandler();
        return new LockingRehydrationImmoter(immoter, invocation, lockHandler);
    }

    protected SMToIMEmoter newSessionRelocationEmoter() {
        return new SMToIMEmoter();
    }

    class SMToIMEmoter implements Emoter {
        public boolean emote(Motable emotable, Motable immotable) {
            return true;
        }
    }

}
