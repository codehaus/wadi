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
package org.codehaus.wadi.activecluster;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;

import org.apache.activecluster.Cluster;
import org.apache.activecluster.ClusterFactory;
import org.apache.activecluster.ClusterListener;
import org.apache.activecluster.Node;
import org.apache.activecluster.impl.DefaultClusterFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.DispatcherConfig;
import org.codehaus.wadi.group.impl.AbstractDispatcher;

/**
 * A Dispatcher for ActiveCluster
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ActiveClusterDispatcher extends AbstractDispatcher {

	protected static String _incomingCorrelationIdKey="incomingCorrelationId";
	protected static String _outgoingCorrelationIdKey="outgoingCorrelationId";

	protected Cluster _cluster;
	protected MessageConsumer _clusterConsumer;
	protected MessageConsumer _nodeConsumer;

	protected final String _clusterUri;

	public ActiveClusterDispatcher(String clusterName, String nodeName, String clusterUri, long inactiveTime) {
		super(clusterName, nodeName, inactiveTime);
		_clusterUri=clusterUri;
		_log=LogFactory.getLog(getClass()+"#"+_nodeName);
	}

	// 5 calls

	public Cluster getCluster() {
		return _cluster;
	}

	// soon to be obsolete...

	public MessageConsumer addDestination(Destination destination) throws JMSException {
		boolean excludeSelf=true;
		MessageConsumer consumer=_cluster.createConsumer(destination, null, excludeSelf);
		consumer.setMessageListener(this);
		return consumer;
	}

	public void removeDestination(MessageConsumer consumer) throws JMSException {
		consumer.close();
	}

	//-----------------------------------------------------------------------------------------------
	// AbstractDispatcher overrides

	protected ActiveMQConnectionFactory _connectionFactory;
	public ClusterFactory _clusterFactory;

	public void init(DispatcherConfig config) throws Exception {
		super.init(config);
		try {
			_connectionFactory=new ActiveMQConnectionFactory(_clusterUri);
			DefaultClusterFactory tmp=new DefaultClusterFactory(_connectionFactory);
			tmp.setInactiveTime(_inactiveTime);
			_clusterFactory=tmp;
			_cluster=_clusterFactory.createCluster(_clusterName);
		} catch (Exception e) {
			_log.error("problem starting Cluster", e);
		}

		boolean excludeSelf;
		excludeSelf=false;
		_clusterConsumer=_cluster.createConsumer(_cluster.getDestination(), null, excludeSelf);
		_clusterConsumer.setMessageListener(this);
		excludeSelf=false;
		_nodeConsumer=_cluster.createConsumer(_cluster.getLocalNode().getDestination(), null, excludeSelf);
		_nodeConsumer.setMessageListener(this);
	}

	//-----------------------------------------------------------------------------------------------
	// Dispatcher API

	public void start() throws Exception {
		_cluster.start();
        Map distributedState = getDistributedState();
        distributedState.put("nodeName", _nodeName);
        setDistributedState(distributedState);
	}

	public void stop() throws Exception {
		// shut down activemq cleanly - what happens if we are running more than one distributable webapp ?
		// there must be an easier way - :-(

		// TODO - sort this out

//		ActiveMQConnection connection=(ActiveMQConnection)((ExtendedCluster)_cluster).getConnection();
//		TransportChannel channel=(connection==null?null:connection.getTransportChannel());
//		BrokerConnector connector=(channel==null?null:channel.getEmbeddedBrokerConnector());
//		BrokerContainer container=(connector==null?null:connector.getBrokerContainer());
//		if (container!=null)
//			container.stop(); // for peer://

		_cluster.stop();
		Thread.sleep(5*1000);
	}

	public int getNumNodes() {
		return _cluster.getNodes().size()+1; // TODO - really inefficient... - allocates a Map
	}

	public ObjectMessage createObjectMessage() throws Exception {
		return _cluster.createObjectMessage();
	}

	public void send(Destination to, ObjectMessage message) throws Exception {
		if (_messageLog.isTraceEnabled()) {
			try {
				_messageLog.trace("outgoing: "+message.getObject()+" {"+getNodeName(message.getJMSReplyTo())+"->"+getNodeName(message.getJMSDestination())+"} - "+getIncomingCorrelationId(message)+"/"+getOutgoingCorrelationId(message)+" on "+Thread.currentThread().getName());
			} catch (JMSException e) {
				_log.warn("problem extracting message content", e);
			}
		}
		_cluster.send(to, message);
	}

    public String getLocalNodeName() {
        Destination localDestination = getLocalDestination();
        String nodeName = getNodeName(localDestination);
        return nodeName;
    }

	public Destination getLocalDestination() {
		return _cluster.getLocalNode().getDestination();
	}

	public Destination getClusterDestination() {
		return _cluster.getDestination();
	}

	public Map getDistributedState() {
		return _cluster.getLocalNode().getState();
	}

	public void setDistributedState(Map state) throws Exception {
		_cluster.getLocalNode().setState(state);
	}

    public Destination getDestination(String nodeName) {
        if (getLocalNodeName().equals(nodeName)) {
            return getLocalDestination();
        }

        Map destToNode = _cluster.getNodes();
        for (Iterator iter = destToNode.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            Destination destination = (Destination) entry.getKey();
            String name = getNodeName(destination);
            if (name.equals(nodeName)) {
                return destination;
            }
        }

        throw new IllegalArgumentException("Node " + nodeName + " is undefined.");
    }

	public String getNodeName(Destination destination) {
		Node localNode=_cluster.getLocalNode();
		Destination localDestination=localNode.getDestination();

		if (destination==null) {
			return "<NULL-DESTINATION>";
		}

		if (destination.equals(localDestination))
			return getNodeName(localNode);

		Destination clusterDestination=_cluster.getDestination();
		if (destination.equals(clusterDestination))
			return "cluster";

		Node node=null;
		if ((node=(Node)_cluster.getNodes().get(destination))!=null)
			return getNodeName(node);

		return "<unknown>";
	}

	public String getIncomingCorrelationId(ObjectMessage message) throws Exception {
		return message.getStringProperty(_incomingCorrelationIdKey);
	}

	public void setIncomingCorrelationId(ObjectMessage message, String id) throws JMSException {
		message.setStringProperty(_incomingCorrelationIdKey, id);
	}

	public String getOutgoingCorrelationId(ObjectMessage message) throws JMSException {
		return message.getStringProperty(_outgoingCorrelationIdKey);
	}

	public void setOutgoingCorrelationId(ObjectMessage message, String id) throws JMSException {
		message.setStringProperty(_outgoingCorrelationIdKey, id);
	}

	public void findRelevantSessionNames(int numPartitions, Collection[] resultSet) {
		throw new UnsupportedOperationException("NYI");
	}

	// temporary

	public void setClusterListener(ClusterListener listener) {
		_cluster.addClusterListener(listener);
	}

}
