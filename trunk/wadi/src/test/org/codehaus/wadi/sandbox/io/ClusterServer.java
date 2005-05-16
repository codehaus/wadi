/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.sandbox.io;

import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import org.codehaus.wadi.Cluster;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

public class ClusterServer extends AbstractServer implements MessageListener {

    protected final Cluster _cluster;
    protected final Map _connections;
    
    public ClusterServer(PooledExecutor executor, Cluster cluster) {
        super(executor);
        _cluster=cluster;
        _connections=new HashMap();
    }
    
    protected MessageConsumer _nodeConsumer;
    
    public void start() throws JMSException {
        boolean excludeSelf;
//        excludeSelf=true;
//        _clusterConsumer=_cluster.createConsumer(_cluster.getDestination(), null, excludeSelf);
//        _clusterConsumer.setMessageListener(this);
        excludeSelf=false;
        _nodeConsumer=_cluster.createConsumer(_cluster.getLocalNode().getDestination(), null, excludeSelf);
        _nodeConsumer.setMessageListener(this);
    }

    public void onMessage(Message message) {
        try {
            if (message instanceof BytesMessage) {
                int length=message.getIntProperty("content-length");
                byte[] buffer=new byte[length];
                ((BytesMessage)message).readBytes(buffer);
                String correlationId=message.getJMSCorrelationID();
                synchronized (_connections) {
                    ClusterConnection connection=(ClusterConnection)_connections.get(correlationId);
                    if (connection==null) {
                        // initialising a new Connection...
                        connection=new ClusterConnection(this, correlationId, new LinkedQueue());
                    }
                    // servicing existing connection...
                    Utils.safePut(buffer, connection);
                }
            }
        } catch (JMSException e) {
            _log.error(e);
        }
    }
    
}
