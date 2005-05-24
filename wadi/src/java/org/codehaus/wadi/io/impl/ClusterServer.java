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
package org.codehaus.wadi.io.impl;

import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import org.codehaus.wadi.ExtendedCluster;
import org.codehaus.wadi.io.Connection;
import org.codehaus.wadi.io.ConnectionConfig;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;

public class ClusterServer extends AbstractServer implements ConnectionConfig, MessageListener {

    protected final ExtendedCluster _cluster;
    protected final boolean _excludeSelf;
    protected final Map _connections;
        
    public ClusterServer(PooledExecutor executor, long connectionTimeout, ExtendedCluster cluster, boolean excludeSelf) {
        super(executor, connectionTimeout);
        _cluster=cluster;
        _excludeSelf=excludeSelf;
        _connections=new HashMap();
    }
    
    protected MessageConsumer _nodeConsumer;
    protected MessageConsumer _clusterConsumer;
    
    public void start() throws Exception {
        super.start();
        _clusterConsumer=_cluster.createConsumer(_cluster.getDestination(), null, _excludeSelf);
        _clusterConsumer.setMessageListener(this);
        _nodeConsumer=_cluster.createConsumer(_cluster.getLocalNode().getDestination(), null, _excludeSelf);
        _nodeConsumer.setMessageListener(this);
    }

    public void stop() throws Exception {
        stopAcceptingConnections();
        waitForExistingConnections();
        super.stop();
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
                //_log.info("receiving message");
                synchronized (_connections) {
                    ClusterConnection connection=(ClusterConnection)_connections.get(correlationId);
                    if (connection==null) {
                        // initialising a new Connection...
                        String name=correlationId.substring(0, correlationId.length()-7);
                        Destination us=_cluster.getLocalNode().getDestination();
                        connection=new ClusterConnection(this, _connectionTimeout, _cluster, us, replyTo, name, new LinkedQueue(), true);
                        //_log.info("created Connection: '"+connection.getCorrelationId()+"'");
                        _connections.put(connection.getCorrelationId(), connection);
                        run(connection);
                    }
                    // servicing existing connection...
                    if (bm.getBodyLength()>0) {
                        //_log.info("servicing Connection: '"+correlationId+"' - "+bm.getBodyLength()+" bytes");
                        Utils.safePut(bm, connection);
                    }
                    if (bm.getBooleanProperty("closing-stream")) {
                        //_log.info("SERVER CLOSING STREAM: "+connection);
                        connection.commit();
                    }
                }
            }
        } catch (JMSException e) {
            _log.error("unexpected problem", e);
        }
    }
    
    // needs more thought...
    
    public Connection makeClientConnection(String correlationId, Destination target) {
        Destination source=_cluster.getLocalNode().getDestination();
        LinkedQueue queue=new LinkedQueue();
        ClusterConnection connection=new ClusterConnection(this, _connectionTimeout, _cluster, source, target, correlationId, queue, false);
        _connections.put(connection.getCorrelationId(), connection);
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
        _connections.remove(((ClusterConnection)connection)._ourCorrelationId); // TODO - encapsulate properly
    }

    public void notifyIdle(Connection connection) {
        // Cluster Connections idle automatically after running...
    }

}
