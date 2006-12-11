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
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.location.SessionResponseMessage;
import org.codehaus.wadi.location.impl.local.LocalPartition.Location;
import org.codehaus.wadi.location.session.EvacuateIMToPM;
import org.codehaus.wadi.location.session.EvacuatePMToIM;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * 
 * @version $Revision: 1538 $
 */
public class LocalPartitionEvacuateIMToPMAction extends AbstractLocalPartitionAction {

    public LocalPartitionEvacuateIMToPMAction(Dispatcher dispatcher, Map nameToLocation, Log log) {
        super(dispatcher, nameToLocation, log);
    }

    public void onMessage(Envelope message, EvacuateIMToPM request) {
        Peer newPeer = request.getPeer();
        Object key = request.getKey();
        Location location;
        synchronized (nameToLocation) {
            location = (Location) nameToLocation.get(key);
        }
        boolean success = false;
        Peer oldPeer = null;
        if (location == null) {
            log.warn("evacuate [" + key + "]@[" + newPeer + "] failed; key not in use");
        } else {
            Sync lock = location.getExclusiveLock();
            try {
                lock.acquire();
                oldPeer = location.getSMPeer();
                if (oldPeer == newPeer) {
                    log.warn("evacuate [" + key + "]@[" + newPeer + "] failed; evacuee is already there");
                } else {
                    location.setPeer(newPeer);
                    if (log.isDebugEnabled()) {
                        log.debug("evacuate [" + request.getKey() + "] [" + oldPeer + "]->[" + newPeer + "]");
                    }
                    success = true;
                }
            } catch (InterruptedException e) {
                log.error("unexpected interruption waiting to perform relocation: " + key, e);
            } finally {
                lock.release();
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
