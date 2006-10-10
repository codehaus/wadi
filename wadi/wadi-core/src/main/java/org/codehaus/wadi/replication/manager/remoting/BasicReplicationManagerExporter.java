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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.message.ResultInfo;

/**
 * 
 * @version $Revision$
 */
public class BasicReplicationManagerExporter implements ReplicationManagerExporter, BasicReplicationManagerExporterMessageListener {
    private static final Log log = LogFactory.getLog(BasicReplicationManagerExporter.class);
    
    private final Dispatcher dispatcher;
    private final ServiceEndpointBuilder endpointBuilder;
    private volatile ReplicationManager manager;
    
    public BasicReplicationManagerExporter(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
        
        endpointBuilder = new ServiceEndpointBuilder();
    }
    
    public void export(ReplicationManager manager) throws Exception {
        this.manager = manager;
        endpointBuilder.addSEI(dispatcher, BasicReplicationManagerExporterMessageListener.class, this);
    }
    
    public void unexport(ReplicationManager manager) throws Exception {
        endpointBuilder.dispose(10, 500);
        this.manager = null;
    }
    
    public void onRequest(Envelope message, ReplicationManagerRequest command) {
        if (null == manager) {
            throw new IllegalStateException("ReplicationManager has been unexported.");
        }
        
        if (command.isOneWay()) {
            command.execute(manager);
        } else {
            ResultInfo resultInfo = command.executeWithResult(manager);
            if (resultInfo.isReplyWithResult()) {
                try {
                    dispatcher.reply(message, new ReleasePrimaryResult(resultInfo.getResult()));
                } catch (MessageExchangeException e) {
                    log.error("See exception", e);
                }
            }
        }
    }
}
