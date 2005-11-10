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

import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.activecluster.Node;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.ActiveClusterDispatcherConfig;
import org.codehaus.wadi.DispatcherConfig;
import org.codehaus.wadi.dindex.impl.DIndex;

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
	
	public ActiveClusterDispatcher() {
		super();
	}
	
	public ActiveClusterDispatcher(String name) {
		this();
		_log=LogFactory.getLog(getClass()+"#"+name);
	}
	
	public Cluster getCluster() {
		return _cluster;
	}
	
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
	
	public void init(DispatcherConfig config) throws Exception {
		super.init(config);
		_cluster=((ActiveClusterDispatcherConfig)_config).getCluster();
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
		_cluster.stop();
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

	public void setDistributedState(Map state) throws Exception {
		_cluster.getLocalNode().setState(state);
	}

	public String getNodeName(Destination destination) {
		Node localNode=_cluster.getLocalNode();
		Destination localDestination=localNode.getDestination();
		
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
	
}
