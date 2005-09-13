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
package org.codehaus.wadi.impl;

import java.io.Serializable;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.activecluster.ClusterFactory;
import org.activecluster.ClusterListener;
import org.activecluster.LocalNode;
import org.activecluster.election.ElectionStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.ExtendedCluster;

/**
 * An ActiveCluster Cluster that can be re-start()-ed after a stop().
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class RestartableCluster implements ExtendedCluster {

    protected static final Log _log=LogFactory.getLog(RestartableCluster.class);

    protected final ClusterFactory _factory;
    protected final Topic _groupDestination;
    protected final String _topic;

    protected ExtendedCluster _cluster;

    public RestartableCluster(ClusterFactory factory, Topic groupDestination) {
        super();
        _factory=factory;
        _groupDestination=groupDestination;
        _topic=null;
        ensureCluster();
    }

    public RestartableCluster(ClusterFactory factory, String topic) {
        super();
        _factory=factory;
        _groupDestination=null;
        _topic=topic;
        ensureCluster();
    }

    // ActiveCluster Cluster

    public Topic getDestination() {
        return _cluster.getDestination();
    }

    public Map getNodes() {
        return _cluster.getNodes();
    }

    public void addClusterListener(ClusterListener listener) {
        ensureCluster();
        _cluster.addClusterListener(listener);
    }


    public void removeClusterListener(ClusterListener listener) {
        ensureCluster();
        _cluster.removeClusterListener(listener);
    }

    public LocalNode getLocalNode() {
        return _cluster.getLocalNode();
    }

    public void send(Destination destination, Message message) throws JMSException {
        _cluster.send(destination, message);
    }

    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        return _cluster.createConsumer(destination);
    }

    public MessageConsumer createConsumer(Destination destination, String selector) throws JMSException {
        return _cluster.createConsumer(destination, selector);
    }

    public MessageConsumer createConsumer(Destination destination, String selector, boolean noLocal) throws JMSException {
        return _cluster.createConsumer(destination, selector, noLocal);
    }

    public Message createMessage() throws JMSException {
        return _cluster.createMessage();
    }

    public BytesMessage createBytesMessage() throws JMSException {
        return _cluster.createBytesMessage();
    }

    public MapMessage createMapMessage() throws JMSException {
        return _cluster.createMapMessage();
    }

    public ObjectMessage createObjectMessage() throws JMSException {
        return _cluster.createObjectMessage();
    }

    public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
        return _cluster.createObjectMessage(object);
    }

    public StreamMessage createStreamMessage() throws JMSException {
        return _cluster.createStreamMessage();
    }

    public TextMessage createTextMessage() throws JMSException {
        return _cluster.createTextMessage();
    }

    public TextMessage createTextMessage(String text) throws JMSException {
        return _cluster.createTextMessage(text);
    }

    public boolean waitForClusterToComplete(int expectedCount, long timeout) throws InterruptedException {
        return _cluster.waitForClusterToComplete(expectedCount, timeout);
    }

    protected void createCluster() {
        try {
            if (_groupDestination!=null)
                _cluster=(ExtendedCluster)_factory.createCluster(_groupDestination);
            else
                _cluster=(ExtendedCluster)_factory.createCluster(_topic);
        } catch (Exception e) {
            _log.error("could not start Cluster", e);
        }
    }

    protected synchronized void ensureCluster() {
        if (_cluster==null)
            createCluster();
    }

    public void start() throws JMSException {
        ensureCluster();
        _cluster.start();
    }

    public void stop() throws JMSException {
        _cluster.stop();
        _cluster=null;
    }

    // ExtendedCluster

    public Destination createQueue(String name) throws JMSException {
        return _cluster.createQueue(name);
    }

    public Connection getConnection() {
        return _cluster.getConnection();
    }

    public void setElectionStrategy(ElectionStrategy strategy) {
       _cluster.setElectionStrategy(strategy);
    }

}
