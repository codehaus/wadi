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
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.location.partitionmanager.local.LocalPartition.Location;
import org.codehaus.wadi.location.session.DeleteIMToPM;
import org.codehaus.wadi.location.session.DeletePMToIM;
import org.codehaus.wadi.location.session.SessionResponseMessage;

/**
 * 
 * @version $Revision: 1538 $
 */
public class LocalPartitionDeleteIMToPMAction  extends AbstractLocalPartitionAction {
    
    public LocalPartitionDeleteIMToPMAction(Dispatcher dispatcher, Map nameToLocation, Log log) {
        super(dispatcher, nameToLocation, log);
    }

    public void onMessage(Envelope message, DeleteIMToPM request) {
        Object key = request.getKey();
        Location location;
        synchronized (nameToLocation) {
            location = (Location) nameToLocation.remove(key);
        }
        boolean success = false;
        if (location != null) {
            success = true;
            if (log.isDebugEnabled()) {
                log.debug("deleted [" + key + "] located at [" + location.getSMPeer() + "]");
            }
        } else {
            log.warn("delete [" + key + "] failed; key not present");
        }

        SessionResponseMessage response = new DeletePMToIM(success);
        try {
            dispatcher.reply(message, response);
        } catch (MessageExchangeException e) {
            log.warn("See exception", e);
        }
    }

}
