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
package org.codehaus.wadi.replication.manager.remoting;

import javax.jms.ObjectMessage;

import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.message.AbstractTwoWayMessage;
import org.codehaus.wadi.replication.message.ResultInfo;

/**
 * 
 * @version $Revision$
 */
public class BasicReplicationManagerExporter implements ReplicationManagerExporter {
    private final Dispatcher dispatcher;
    private volatile ReplicationManager manager;

    public BasicReplicationManagerExporter(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
    
    public void export(ReplicationManager manager) throws Exception {
        this.manager = manager;
        dispatcher.register(this, "onRequest", ReplicationManagerRequest.class);
    }
    
    public void unexport(ReplicationManager manager) throws Exception {
        // TODO signature seems to be wrong.
        dispatcher.deregister("onRequest", ReplicationManagerRequest.class, (int) AbstractTwoWayMessage.DEFAULT_TWO_WAY_TIMEOUT);
        this.manager = null;
    }
    
    public void onRequest(ObjectMessage message, ReplicationManagerRequest command) {
        if (null == manager) {
            throw new IllegalStateException("ReplicationManager has been unexported.");
        }
        
        if (command.isOneWay()) {
            command.execute(manager);
        } else {
            ResultInfo resultInfo = command.executeWithResult(manager);
            if (resultInfo.isReplyWithResult()) {
                dispatcher.reply(message, new ReleasePrimaryResult(resultInfo.getResult()));
            }
        }
    }
}
