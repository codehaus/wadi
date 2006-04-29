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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.replication.common.ComponentEventType;
import org.codehaus.wadi.replication.common.NodeInfo;
import org.codehaus.wadi.replication.message.ResultInfo;
import org.codehaus.wadi.replication.storage.ReplicaStorage;


/**
 * 
 * @version $Revision$
 */
public class BasicReplicaStorageExporter implements ReplicaStorageExporter, BasicReplicaStorageExporterMessageListener {
    private static final Log log = LogFactory.getLog(BasicReplicaStorageExporter.class); 
    
    private final Dispatcher dispatcher;
    private final BasicReplicaStorageAdvertiser adviser;
    private volatile ReplicaStorage storage;

    private final ServiceEndpointBuilder endpointBuilder;

    public BasicReplicaStorageExporter(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
        
        adviser = new BasicReplicaStorageAdvertiser(dispatcher);
        
        endpointBuilder = new ServiceEndpointBuilder();
    }
    
    public void export(ReplicaStorage storage) throws Exception {
        this.storage = storage;
        
        endpointBuilder.addSEI(dispatcher, BasicReplicaStorageExporterMessageListener.class, this);
        adviser.advertiseJoin(storage);
    }
    
    public void unexport(ReplicaStorage storage) throws Exception {
        this.storage = null;

        endpointBuilder.dispose(10, 500);
        adviser.advertiseLeave(storage);
    }
    
    public void onCommand(Message message, ReplicaStorageRequest command) {
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
                try {
                    dispatcher.reply(message, new ReplicaStorageResult(resultInfo.getResult()));
                } catch (MessageExchangeException e) {
                    log.error("See exception", e);
                }
            }
        }
    }

    public void onMonitorEvent(Message message, ReplicaStorageMonitorEvent event) {
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
