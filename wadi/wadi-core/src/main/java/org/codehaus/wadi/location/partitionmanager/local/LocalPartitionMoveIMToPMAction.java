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
package org.codehaus.wadi.location.partitionmanager.local;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.codehaus.wadi.core.WADIRuntimeException;
import org.codehaus.wadi.core.util.Lease;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.location.partitionmanager.local.LocalPartition.Location;
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

    public LocalPartitionMoveIMToPMAction(Dispatcher dispatcher, Map nameToLocation, Log log) {
        super(dispatcher, nameToLocation, log);
    }

    public void onMessage(Envelope message, MoveIMToPM request) {
        Object key = request.getKey();
        try {
            Location location;
            synchronized (nameToLocation) {
                location = (Location) nameToLocation.get(key);
            }
            if (location == null) {
                replyWithUnknownLocation(message);
                return;
            }
            
            Peer pmPeer = dispatcher.getCluster().getLocalPeer();
            String sourceCorrelationId = message.getSourceCorrelationId();
            boolean relocateSession = request.isRelocateSession();
            if (relocateSession) {
                relocateSession(message, location, request, sourceCorrelationId);
            } else {
                relocateInvocation(location, request, pmPeer, sourceCorrelationId);
            }
        } catch (Exception e) {
            log.error("UNEXPECTED PROBLEM RELOCATING STATE: " + key);
        }
    }

    protected void relocateSession(Envelope message, Location location, MoveIMToPM request, String imCorrelationId)
            throws MessageExchangeException {
        Object key = location.getKey();
        // session does exist - we need to ask SM to move it to IM
        Sync lock = location.getExclusiveLock();
        try {
            // ensures that no-one else tries to relocate session whilst we are doing so...
            // wait til we have a lock on Location before retrieving the SM
            lock.acquire();
            try {
                doRelocateSession(message, location, request, imCorrelationId);
            } finally {
                lock.release();
            }
        } catch (InterruptedException e) {
            log.error("unexpected interruption waiting to perform Session relocation: " + key, e);
            Thread.currentThread().interrupt();
        }
    }

    protected void doRelocateSession(Envelope message, Location location, MoveIMToPM request, String imCorrelationId)
            throws MessageExchangeException {
        Peer imPeer = request.getIMPeer();
        Object key = location.getKey();
        Peer smPeer = location.getSMPeer();
        if (smPeer == imPeer) {
            throw new WADIRuntimeException("session [" + key + "] already at [" + imPeer + "]; should not happen");
        }
        
        long exclusiveSessionLockWaitTime = request.getExclusiveSessionLockWaitTime();
        MovePMToSM pmToSMRequest = new MovePMToSM(key, imPeer, imCorrelationId, exclusiveSessionLockWaitTime);
        Envelope tmp;
        try {
            tmp = dispatcher.exchangeSend(smPeer.getAddress(), 
                pmToSMRequest, 
                exclusiveSessionLockWaitTime + 5000);
        } catch (MessageExchangeException e) {
            log.error("move [" + key + "]@[" + smPeer + "]->[" + imPeer + "] failed", e);
            replyWithUnknownLocation(message);
            synchronized (nameToLocation) {
                location = (Location) nameToLocation.remove(key);
            }
            return;
        }
        
        MoveSMToPM response = (MoveSMToPM) tmp.getPayload();
        if (response.isSuccess()) {
            // alter location
            location.setPeer(imPeer);
            if (log.isDebugEnabled()) {
                log.debug("move [" + key + "]@[" + smPeer + "]->[" + imPeer + "]");
            }
        } else if (response.isSessionBuzy()) {
            log.warn("Motable buzy. Move [" + key + "]@[" + smPeer + "]->[" + imPeer + "] aborted.");
        } else {
            replyWithUnknownLocation(message);
            synchronized (nameToLocation) {
                location = (Location) nameToLocation.remove(key);
            }
            log.warn("move [" + key + "]@[" + smPeer + "]->[" + imPeer + "] failed");
        }
    }

    protected void relocateInvocation(Location location, MoveIMToPM request, Peer pmPeer, String imCorrelationId)
            throws MessageExchangeException {
        Peer imPeer = request.getIMPeer();
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

    protected void replyWithUnknownLocation(Envelope message) throws MessageExchangeException {
        dispatcher.reply(message, new MovePMToIM());
    }
    
}
