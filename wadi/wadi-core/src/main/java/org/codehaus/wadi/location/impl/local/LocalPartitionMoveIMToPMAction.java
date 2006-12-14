/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.location.impl.local;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.codehaus.wadi.Lease;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.impl.WADIRuntimeException;
import org.codehaus.wadi.location.impl.local.LocalPartition.Location;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.MovePMToIM;
import org.codehaus.wadi.location.session.MovePMToIMInvocation;
import org.codehaus.wadi.location.session.MovePMToSM;
import org.codehaus.wadi.location.session.MoveSMToPM;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * 
 * @version $Revision: 1538 $
 */
public class LocalPartitionMoveIMToPMAction extends AbstractLocalPartitionAction {
    private final long relocationTimeout;

    public LocalPartitionMoveIMToPMAction(Dispatcher dispatcher, Map nameToLocation, Log log, long relocationTimeout) {
        super(dispatcher, nameToLocation, log);
        if (1 > relocationTimeout) {
            throw new IllegalArgumentException("relocationTimeout must be positive");
        }
        this.relocationTimeout = relocationTimeout;
    }

    public void onMessage(Envelope message, MoveIMToPM request) {
        Object key = request.getKey();
        try {
            Location location;
            synchronized (nameToLocation) {
                location = (Location) nameToLocation.get(key);
            }
            if (location == null) {
                // session does not exist - tell IM
                dispatcher.reply(message, new MovePMToIM());
                return;
            }
            // we need to make a decision here - based on the info available to us...
            // are we going to relocate the Session to the Invocation or the Invocation to the Session ?
            // call out to a pluggable strategy...
            // we need to know whether the IM's LBPolicy supports 'resticking' - otherwise relocating invocation is 
            // not such a smart thing to do...
            // if the InvocationMaster is shuttingDown, we know we should relocate the Invocation - lets go with that 
            // for now...
            // if the StateMaster is shuttingDown, we know we should relocate the session - but how would we know ?
            Peer imPeer = request.getIMPeer();
            Peer pmPeer = dispatcher.getCluster().getLocalPeer();
            String sourceCorrelationId = message.getSourceCorrelationId();
            boolean relocateSession = request.isRelocateSession();
            if (relocateSession) {
                relocateSession(location, imPeer, sourceCorrelationId);
            } else {
                relocateInvocation(location, imPeer, pmPeer, sourceCorrelationId);
            }
        } catch (Exception e) {
            log.error("UNEXPECTED PROBLEM RELOCATING STATE: " + key);
        }
    }

    protected void relocateSession(Location location, Peer imPeer, String imCorrelationId)
            throws MessageExchangeException {
        Object key = location.getKey();
        // session does exist - we need to ask SM to move it to IM
        Sync lock = location.getExclusiveLock();
        try {
            // ensures that no-one else tries to relocate session whilst we are doing so...
            // wait til we have a lock on Location before retrieving the SM
            lock.acquire();
            try {
                doRelocateSession(location, imPeer, imCorrelationId);
            } finally {
                lock.release();
            }
        } catch (InterruptedException e) {
            log.error("unexpected interruption waiting to perform Session relocation: " + key, e);
            Thread.currentThread().interrupt();
        }
    }

    protected void doRelocateSession(Location location, Peer imPeer, String imCorrelationId)
            throws MessageExchangeException {
        Object key = location.getKey();
        Peer smPeer = location.getSMPeer();
        if (smPeer == imPeer) {
            // session does exist - but is already located at the IM
            // whilst we were waiting for the partition lock, another thread
            // must have migrated the session to the IM...
            // How can this happen - the first Thread should have been
            // holding the InvocationLock...
            throw new WADIRuntimeException("session [" + key + "] already at [" + imPeer + "]; should not happen");
        }
        
        MovePMToSM request = new MovePMToSM(key, imPeer, imCorrelationId);
        Envelope tmp;
        try {
            tmp = dispatcher.exchangeSend(smPeer.getAddress(), request, relocationTimeout);
        } catch (MessageExchangeException e) {
            log.error("move [" + key + "]@[" + smPeer + "]->[" + imPeer + "] failed", e);
            return;
        }
        
        MoveSMToPM response = (MoveSMToPM) tmp.getPayload();
        if (response.getSuccess()) {
            // alter location
            location.setPeer(imPeer);
            if (log.isDebugEnabled()) {
                log.debug("move [" + key + "]@[" + smPeer + "]->[" + imPeer + "]");
            }
        } else {
            log.warn("move [" + key + "]@[" + smPeer + "]->[" + imPeer + "] failed");
        }
    }
    
    protected void relocateInvocation(Location location, Peer imPeer, Peer pmPeer, String imCorrelationId)
            throws MessageExchangeException {
        Object key = location.getKey();
        long leasePeriod = 5000;
        try {
            Lease.Handle handle = location.getSharedLease().acquire(leasePeriod);
            // wait til we have a lock on Location before retrieving the SM
            Peer smPeer = location.getSMPeer(); 
            if (smPeer == imPeer) {
                // do something similar to above - but remember - we only have a lease...
                log.warn("session [" + key + "] already at [" + imPeer + "]; should not happen");
            }
            // send a message back to the IM, informing it that it has a lease
            // and should relocate its invocation to the SM...
            MovePMToIMInvocation response = new MovePMToIMInvocation(handle, leasePeriod, smPeer);
            dispatcher.reply(pmPeer.getAddress(), imPeer.getAddress(), imCorrelationId, response);
        } catch (InterruptedException e) {
            log.error("unexpected interruption waiting to perform Invocation relocation: " + key, e);
        }
    }
    
}
