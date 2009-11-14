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
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.location.session.EvacuateIMToPM;
import org.codehaus.wadi.location.session.EvacuatePMToIM;
import org.codehaus.wadi.location.session.SessionResponseMessage;


/**
 * 
 * @version $Revision: 1538 $
 */
public class LocalPartitionEvacuateIMToPMAction extends AbstractLocalPartitionAction {

    public LocalPartitionEvacuateIMToPMAction(Dispatcher dispatcher, Map<Object, Location> nameToLocation, Log log) {
        super(dispatcher, nameToLocation, log);
    }

    public void onMessage(Envelope message, EvacuateIMToPM request) {
        Peer newPeer = request.getPeer();
        Object key = request.getId();
        Location location;
        synchronized (nameToLocation) {
            location = (Location) nameToLocation.get(key);
        }
        boolean success = false;
        Peer oldPeer = null;
        if (location == null) {
            log.warn("evacuate [" + key + "]@[" + newPeer + "] failed; key not in use");
        } else {
            Lock lock = location.getExclusiveLock();
            try {
                lock.lockInterruptibly();
                try {
                    oldPeer = location.getSMPeer();
                    if (oldPeer == newPeer) {
                        log.warn("evacuate [" + key + "]@[" + newPeer + "] failed; evacuee is already there");
                    } else {
                        location.setPeer(newPeer);
                        if (log.isDebugEnabled()) {
                            log.debug("evacuate [" + request.getId() + "] [" + oldPeer + "]->[" + newPeer + "]");
                        }
                        success = true;
                    }
                } finally {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                log.error("unexpected interruption waiting to perform relocation: " + key, e);
                Thread.currentThread().interrupt();
            }
        }

        SessionResponseMessage response = new EvacuatePMToIM(success);
        try {
            dispatcher.reply(message, response);
        } catch (MessageExchangeException e) {
            log.warn("See exception", e);
        }
    }
    
}
