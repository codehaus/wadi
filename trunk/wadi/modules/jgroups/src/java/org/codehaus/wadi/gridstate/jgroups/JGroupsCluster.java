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
import java.util.ArrayList;
import java.util.List;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Channel;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JGroupsCluster implements Cluster {

  protected final Log _log = LogFactory.getLog(getClass());
  protected final List _clusterListeners=new ArrayList();
  protected final JGroupsTopic _clusterTopic=new JGroupsTopic("CLUSTER", null);

  protected ElectionStrategy _electionStrategy;
  protected LocalNode _localNode;
  protected Channel _channel;

	public JGroupsCluster() {
		super();
	}

  // 'Cluster' api
  
	public Topic getDestination() {
	  return _clusterTopic;
	}

	public Map getNodes() {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public void addClusterListener(ClusterListener listener) {
	  synchronized (_clusterListeners) {
	    _clusterListeners.add(listener);
	  }
	}
	
	public void removeClusterListener(ClusterListener listener) {
	  synchronized (_clusterListeners) {
	    _clusterListeners.remove(listener);
	  }
	}

	public LocalNode getLocalNode() {
	  return _localNode;
	}

	public void setElectionStrategy(ElectionStrategy strategy) {
		_electionStrategy=strategy;
	}

	public void send(Destination destination, Message message) throws JMSException {
	  try {
	    _channel.send(null, ((JGroupsDestination)destination).getAddress(), (JGroupsObjectMessage)message); // broadcast
	  } catch (Exception e) {
	    JMSException jmse=new JMSException("unexpected JGroups problem");
	    jmse.setLinkedException(e);
	    throw jmse;
	  }
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

  // 'JGroupsCluster' api
  
  public void init(Channel channel) throws Exception {
    _channel=channel;
    _localNode=new JGroupsLocalNode(this, new JGroupsDestination(_channel.getLocalAddress()));
  }
  
}
