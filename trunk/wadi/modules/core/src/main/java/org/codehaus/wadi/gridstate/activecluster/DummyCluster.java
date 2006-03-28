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
package org.codehaus.wadi.gridstate.activecluster;

import java.io.Serializable;
import java.util.Collections;
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

import org.apache.activecluster.Cluster;
import org.apache.activecluster.ClusterListener;
import org.apache.activecluster.LocalNode;
import org.apache.activecluster.election.ElectionStrategy;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DummyCluster implements Cluster {

    public DummyCluster() {
        super();
        // TODO Auto-generated constructor stub
    }

    public Destination createQueue(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public Destination getDestination() {
        // TODO Auto-generated method stub
        return null;
    }

    public Map getNodes() {
        return Collections.EMPTY_MAP;
    }

    public void addClusterListener(ClusterListener listener) {
        // TODO Auto-generated method stub

    }

    public void removeClusterListener(ClusterListener listener) {
        // TODO Auto-generated method stub

    }

    protected final LocalNode _localNode=new DummyLocalNode();

    public LocalNode getLocalNode() {
        return _localNode;
    }

    public void send(Destination destination, Message message) {
        // TODO Auto-generated method stub
    }

    public MessageConsumer createConsumer(Destination destination) {
        // TODO Auto-generated method stub
        return null;
    }

    public MessageConsumer createConsumer(Destination destination, String selector) {
        // TODO Auto-generated method stub
        return null;
    }

    public MessageConsumer createConsumer(Destination destination, String selector, boolean noLocal) {
        // TODO Auto-generated method stub
        return null;
    }

    public Message createMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    public BytesMessage createBytesMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    public MapMessage createMapMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    public ObjectMessage createObjectMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    public ObjectMessage createObjectMessage(Serializable object) {
        // TODO Auto-generated method stub
        return null;
    }

    public StreamMessage createStreamMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    public TextMessage createTextMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    public TextMessage createTextMessage(String text) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean waitForClusterToComplete(int expectedCount, long timeout) {
        // TODO Auto-generated method stub
        return false;
    }

    public void start() {
        // TODO Auto-generated method stub

    }

    public void stop() {
        // TODO Auto-generated method stub

    }

    public Connection getConnection() { return null; }

    public void setElectionStrategy(ElectionStrategy strategy) {}

	public Destination createDestination(String name) throws JMSException {
		throw new UnsupportedOperationException();
	}
}
