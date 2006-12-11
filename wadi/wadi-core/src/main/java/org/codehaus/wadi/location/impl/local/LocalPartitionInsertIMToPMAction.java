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
import org.codehaus.wadi.location.session.InsertIMToPM;
import org.codehaus.wadi.location.session.InsertPMToIM;

/**
 * 
 * @version $Revision: 1538 $
 */
public class LocalPartitionInsertIMToPMAction extends AbstractLocalPartitionAction {
    
    public LocalPartitionInsertIMToPMAction(Dispatcher dispatcher, Map nameToLocation, Log log) {
        super(dispatcher, nameToLocation, log);
    }

    public void onMessage(Envelope message, InsertIMToPM request) {
        Object key = request.getKey();
        Peer newPeer = request.getPeer();
        boolean success = false;
        Location newLocation = new Location(key, newPeer);
        synchronized (nameToLocation) {
            // remember location of new session
            Location oldLocation = (Location) nameToLocation.put(key, newLocation);
            if (oldLocation == null) {
                // id was not already in use - expected outcome
                success = true;
                if (log.isDebugEnabled()) {
                    log.debug("inserted [" + key + "]@[" + newPeer + "]");
                }
            } else {
                // id was already in use - unexpected outcome - put it back and forget new location
                nameToLocation.put(key, oldLocation);
                log.warn("insert [" + key + "]@[" + newPeer + "] failed; key already in use");
            }
        }

        SessionResponseMessage response = new InsertPMToIM(success);
        try {
            dispatcher.reply(message, response);
        } catch (MessageExchangeException e) {
            log.warn("See exception", e);
        }
    }
    
}
