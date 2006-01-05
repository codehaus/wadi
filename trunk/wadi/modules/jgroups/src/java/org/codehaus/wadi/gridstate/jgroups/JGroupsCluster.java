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
package org.codehaus.wadi.gridstate.jgroups;

import java.io.Serializable;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.activecluster.Cluster;
import org.activecluster.ClusterListener;
import org.activecluster.LocalNode;
import org.activecluster.election.ElectionStrategy;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JGroupsCluster implements Cluster {

	public JGroupsCluster() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Topic getDestination() {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public Map getNodes() {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public void addClusterListener(ClusterListener listener) {
		throw new UnsupportedOperationException("NYI");
	}

	public void removeClusterListener(ClusterListener listener) {
		throw new UnsupportedOperationException("NYI");
	}

	public LocalNode getLocalNode() {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public void setElectionStrategy(ElectionStrategy strategy) {
		throw new UnsupportedOperationException("NYI");
	}

	public void send(Destination destination, Message message) throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}

	public MessageConsumer createConsumer(Destination destination) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public MessageConsumer createConsumer(Destination destination, String selector) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public MessageConsumer createConsumer(Destination destination, String selector, boolean noLocal) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public Message createMessage() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public BytesMessage createBytesMessage() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public MapMessage createMapMessage() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public ObjectMessage createObjectMessage() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public StreamMessage createStreamMessage() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public TextMessage createTextMessage() throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public TextMessage createTextMessage(String text) throws JMSException {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public boolean waitForClusterToComplete(int expectedCount, long timeout) throws InterruptedException {
		throw new UnsupportedOperationException("NYI");
		//return false;
	}

	public void start() throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}

	public void stop() throws JMSException {
		throw new UnsupportedOperationException("NYI");
	}

}
