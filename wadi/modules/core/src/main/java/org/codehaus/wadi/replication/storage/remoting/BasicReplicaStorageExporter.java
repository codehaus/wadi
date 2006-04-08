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
package org.codehaus.wadi.replication.storage.remoting;

import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.replication.common.ComponentEventType;
import org.codehaus.wadi.replication.common.NodeInfo;
import org.codehaus.wadi.replication.message.ResultInfo;
import org.codehaus.wadi.replication.storage.ReplicaStorage;


/**
 * 
 * @version $Revision$
 */
public class BasicReplicaStorageExporter implements ReplicaStorageExporter {
    private static final Log log = LogFactory.getLog(BasicReplicaStorageExporter.class); 
    
    private final Dispatcher dispatcher;
    private final BasicReplicaStorageAdvertiser adviser;
    private volatile ReplicaStorage storage;

    public BasicReplicaStorageExporter(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
        
        adviser = new BasicReplicaStorageAdvertiser(dispatcher);
    }
    
    public void export(ReplicaStorage storage) throws Exception {
        this.storage = storage;
        
        dispatcher.register(this, "onCommand", ReplicaStorageRequest.class);
        adviser.advertiseJoin(storage);
        
        dispatcher.register(this, "onMonitorEvent", ReplicaStorageMonitorEvent.class);
    }
    
    public void unexport(ReplicaStorage storage) throws Exception {
        this.storage = null;

        dispatcher.deregister("onMonitorEvent", ReplicaStorageMonitorEvent.class, 1000);

        adviser.advertiseLeave(storage);
        dispatcher.deregister("onCommand", ReplicaStorageRequest.class, 1000);
    }
    
    public void onCommand(ObjectMessage message, ReplicaStorageRequest command) {
        if (null == storage) {
            // TODO do not die silently.
            log.warn("Request " + command + " received and storage not set.");
            return;
        }
        
        if (command.isOneWay()) {
            command.execute(storage);
        } else {
            ResultInfo resultInfo = command.executeWithResult(storage);
            if (resultInfo.isReplyWithResult()) {
                dispatcher.reply(message, new ReplicaStorageResult(resultInfo.getResult()));
            }
        }
    }

    public void onMonitorEvent(ObjectMessage message, ReplicaStorageMonitorEvent event) {
        if (null == storage) {
            // TODO do not die silently.
            log.warn("Event " + event + " received and storage not set.");
            return;
        }
        
        if (event.getType() == ComponentEventType.JOIN) {
            NodeInfo hostingNode = event.getHostingNode();
            if (hostingNode.equals(storage.getHostingNode())) {
                return;
            }
            
            adviser.advertiseJoin(storage, hostingNode);
        }
    }
}
