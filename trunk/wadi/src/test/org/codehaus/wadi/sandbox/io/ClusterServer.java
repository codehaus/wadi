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
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import org.codehaus.wadi.Cluster;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

public class ClusterServer extends AbstractServer implements ConnectionConfig, MessageListener {

    protected final Cluster _cluster;
    protected final boolean _excludeSelf;
    protected final Map _connections;
    
    public ClusterServer(PooledExecutor executor, Cluster cluster, boolean excludeSelf) {
        super(executor);
        _cluster=cluster;
        _excludeSelf=excludeSelf;
        _connections=new HashMap();
    }
    
    protected MessageConsumer _nodeConsumer;
    protected MessageConsumer _clusterConsumer;
    
    public void start() throws JMSException {
        _clusterConsumer=_cluster.createConsumer(_cluster.getDestination(), null, _excludeSelf);
        _clusterConsumer.setMessageListener(this);
        _nodeConsumer=_cluster.createConsumer(_cluster.getLocalNode().getDestination(), null, _excludeSelf);
        _nodeConsumer.setMessageListener(this);
    }

    public void stop() {
        stopAcceptingConnections();
        waitForExistingConnections();
    }
    
    public void stopAcceptingConnections() {
        try {
        _clusterConsumer.setMessageListener(null);
        _nodeConsumer.setMessageListener(null);
        } catch (JMSException e) {
            _log.warn("could not remove Listeners", e);
        }
    }
    
    public void onMessage(Message message) {
        try {
            if (message instanceof BytesMessage) {
                BytesMessage bm=(BytesMessage)message;
                String correlationId=bm.getJMSCorrelationID();
                Destination replyTo=bm.getJMSReplyTo();
                synchronized (_connections) {
                    ClusterConnection connection=(ClusterConnection)_connections.get(correlationId);
                    if (connection==null) {
                        // initialising a new Connection...
                        //_log.info("creating Connection: '"+correlationId+"'");
                        connection=new ClusterConnection(this, _cluster, _cluster.getLocalNode().getDestination(), replyTo, correlationId, new LinkedQueue(), 2000);
                        _connections.put(correlationId, connection);
                        doConnection(connection);
                    }
                    // servicing existing connection...
                    //_log.info("servicing Connection: '"+correlationId+"' - "+buffer.length+" bytes");
                    Utils.safePut(bm, connection);
                }
            }
        } catch (JMSException e) {
            _log.error(e);
        }
    }
    
    // needs more thought...
    
    public Connection makeClientConnection(String correlationId, Destination target) {
        ClusterConnection connection=new ClusterConnection(this, _cluster, _cluster.getLocalNode().getDestination(), target, correlationId, new LinkedQueue(), 2000);
        _connections.put(correlationId, connection);
        return connection;
    }
    
    public void waitForExistingConnections() {
        int numConnections;
        while ((numConnections=_connections.size())>0) {
            _log.info("waiting for: "+numConnections+" Connection[s]");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                _log.trace("unexpected interruption - ignoring", e);
            }
        }
        _log.info("existing Connections have finished running");
    }
    
    // ConnectionConfig
    
    public void notifyClosed(Connection connection) {
        _connections.remove(((ClusterConnection)connection)._correlationId);
    }

    public void notifyIdle(Connection connection) {
        // TODO - should we don anything here - ClusterConnections are automatically idle ?
    }

}
