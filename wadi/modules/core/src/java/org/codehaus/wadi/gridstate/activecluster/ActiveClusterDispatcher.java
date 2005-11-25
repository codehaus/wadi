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

import java.util.Collection;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.activecluster.ClusterListener;
import org.activecluster.Node;
import org.activemq.ActiveMQConnection;
import org.activemq.ActiveMQConnectionFactory;
import org.activemq.broker.BrokerConnector;
import org.activemq.broker.BrokerContainer;
import org.activemq.store.vm.VMPersistenceAdapterFactory;
import org.activemq.transport.TransportChannel;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.dindex.impl.DIndex;
import org.codehaus.wadi.gridstate.DispatcherConfig;
import org.codehaus.wadi.gridstate.ExtendedCluster;
import org.codehaus.wadi.gridstate.impl.AbstractDispatcher;

/**
 * A Dispatcher for ActiveCluster
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ActiveClusterDispatcher extends AbstractDispatcher implements MessageListener {

	protected static String _incomingCorrelationIdKey="incomingCorrelationId";
	protected static String _outgoingCorrelationIdKey="outgoingCorrelationId";

	protected Cluster _cluster;
	protected MessageConsumer _clusterConsumer;
	protected MessageConsumer _nodeConsumer;

    protected final String _clusterUri;

	public ActiveClusterDispatcher(String nodeName, String clusterName, String clusterUri, long inactiveTime) {
		super(nodeName, clusterName, inactiveTime);
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
    public CustomClusterFactory _clusterFactory;

	public void init(DispatcherConfig config) throws Exception {
		super.init(config);
        try {
            _connectionFactory=new ActiveMQConnectionFactory(_clusterUri);
            _connectionFactory.start();
    		System.setProperty("activemq.persistenceAdapterFactory", VMPersistenceAdapterFactory.class.getName()); // do we need this ?
            _clusterFactory=new CustomClusterFactory(_connectionFactory);
            _clusterFactory.setInactiveTime(_inactiveTime);
            _cluster=(ExtendedCluster)_clusterFactory.createCluster(_clusterName);
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
	}

	public void stop() throws Exception {
        // shut down activemq cleanly - what happens if we are running more than one distributable webapp ?
        // there must be an easier way - :-(
        ActiveMQConnection connection=(ActiveMQConnection)((ExtendedCluster)_cluster).getConnection();
        TransportChannel channel=(connection==null?null:connection.getTransportChannel());
        BrokerConnector connector=(channel==null?null:channel.getEmbeddedBrokerConnector());
        BrokerContainer container=(connector==null?null:connector.getBrokerContainer());
        if (container!=null)
            container.stop(); // for peer://

		_cluster.stop();
        _connectionFactory.stop();

        Thread.sleep(5*1000);
	}

	public int getNumNodes() {
		return _cluster.getNodes().size()+1; // TODO - really inefficient... - allocates a Map
	}

	public ObjectMessage createObjectMessage() throws Exception {
		return _cluster.createObjectMessage();
	}

	public void send(Destination to, ObjectMessage message) throws Exception {
		_cluster.send(to, message);
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

	public String getNodeName(Destination destination) {
		Node localNode=_cluster.getLocalNode();
		Destination localDestination=localNode.getDestination();

		if (destination==null) {
			return "<NULL-DESTINATION>";
		}
		
		if (destination.equals(localDestination))
			return DIndex.getNodeName(localNode);

		Destination clusterDestination=_cluster.getDestination();
		if (destination.equals(clusterDestination))
			return "cluster";

		Node node=null;
		if ((node=(Node)_cluster.getNodes().get(destination))!=null)
			return DIndex.getNodeName(node);

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
