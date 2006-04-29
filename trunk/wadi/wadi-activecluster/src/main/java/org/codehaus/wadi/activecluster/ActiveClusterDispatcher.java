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
import org.apache.activecluster.ClusterFactory;
import org.apache.activecluster.LocalNode;
import org.apache.activecluster.Node;
import org.apache.activecluster.impl.DefaultClusterFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.DispatcherConfig;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.impl.AbstractDispatcher;

/**
 * A Dispatcher for ActiveCluster
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ActiveClusterDispatcher extends AbstractDispatcher {
	protected org.apache.activecluster.Cluster _cluster;
	protected MessageConsumer _clusterConsumer;
	protected MessageConsumer _nodeConsumer;

	protected final String _clusterUri;
    private final WADIMessageListener _messageListener;

    private Cluster _wadiCluster;
    protected org.codehaus.wadi.group.LocalPeer _localNode;
    
	public ActiveClusterDispatcher(String clusterName, String nodeName, String clusterUri, long inactiveTime) {
		super(clusterName, nodeName, inactiveTime);
		_clusterUri=clusterUri;
		_log=LogFactory.getLog(getClass()+"#"+_nodeName);
        
        _messageListener = new WADIMessageListener(this);
	}

	// 5 calls
	public Cluster getCluster() {
		return _wadiCluster;
	}

	// soon to be obsolete...

	public MessageConsumer addDestination(Destination destination) throws JMSException {
		boolean excludeSelf=true;
		MessageConsumer consumer=_cluster.createConsumer(destination, null, excludeSelf);
		consumer.setMessageListener(_messageListener);
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
		_clusterConsumer.setMessageListener(_messageListener);
		excludeSelf=false;
		_nodeConsumer=_cluster.createConsumer(_cluster.getLocalNode().getDestination(), null, excludeSelf);
		_nodeConsumer.setMessageListener(_messageListener);

        _wadiCluster = new ACClusterAdapter(_cluster);
        _localNode = new ACLocalNodeAdapter(_cluster.getLocalNode());
	}

	//-----------------------------------------------------------------------------------------------
	// Dispatcher API

	public void start() throws MessageExchangeException {
		try {
            _cluster.start();
        } catch (JMSException e) {
            throw new MessageExchangeException(e);
        }
        Map distributedState = getDistributedState();
        distributedState.put("nodeName", _nodeName);
        setDistributedState(distributedState);
	}

	public void stop() throws MessageExchangeException {
		// shut down activemq cleanly - what happens if we are running more than one distributable webapp ?
		// there must be an easier way - :-(

		// TODO - sort this out

//		ActiveMQConnection connection=(ActiveMQConnection)((ExtendedCluster)_cluster).getConnection();
//		TransportChannel channel=(connection==null?null:connection.getTransportChannel());
//		BrokerConnector connector=(channel==null?null:channel.getEmbeddedBrokerConnector());
//		BrokerContainer container=(connector==null?null:connector.getBrokerContainer());
//		if (container!=null)
//			container.stop(); // for peer://

		try {
            _cluster.stop();
            Thread.sleep(5*1000);
        } catch (JMSException e) {
            throw new MessageExchangeException(e);
        } catch (InterruptedException e) {
            throw new MessageExchangeException(e);
        }
	}

	public int getNumNodes() {
		return _cluster.getNodes().size()+1; // TODO - really inefficient... - allocates a Map
	}

	public Message createMessage() {
		try {
            return new ACObjectMessageAdapter(_cluster.createObjectMessage());
        } catch (JMSException e) {
            throw new RuntimeJMSException(e);
        }
	}

	public void send(Address to, Message message) throws MessageExchangeException {
		if (_messageLog.isTraceEnabled()) {
            _messageLog.trace("outgoing: "+message.getPayload()+" {"+getInternalPeerName(message.getReplyTo())+"->"+getInternalPeerName(message.getAddress())+"} - "+message.getIncomingCorrelationId()+"/"+message.getOutgoingCorrelationId()+" on "+Thread.currentThread().getName());
		}
		try {
            _cluster.send(ACDestinationAdapter.unwrap(to), ACObjectMessageAdapter.unwrap(message));
        } catch (JMSException e) {
            throw new MessageExchangeException(e);
        }
	}

	public Address getLocalAddress() {
		return _localNode.getAddress();
	}

	public Address getClusterAddress() {
		return _wadiCluster.getAddress();
	}

	public Map getDistributedState() {
		return _cluster.getLocalNode().getState();
	}

	public void setDistributedState(Map state) throws MessageExchangeException {
		try {
            _cluster.getLocalNode().setState(state);
        } catch (JMSException e) {
            throw new MessageExchangeException(e);
        }
	}

    public Address getAddress(String nodeName) {
        if (getLocalPeer().getName().equals(nodeName)) {
            return _localNode.getAddress();
        }

        Map destToNode = _cluster.getNodes();
        for (Iterator iter = destToNode.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            Destination destination = (Destination) entry.getKey();
            String name = getInternalPeerName(destination);
            if (name.equals(nodeName)) {
                return new ACDestinationAdapter(destination);
            }
        }

        throw new IllegalArgumentException("Node " + nodeName + " is undefined.");
    }

    public String getPeerName(Address address) {
        return getInternalPeerName(ACDestinationAdapter.unwrap(address));
    }

    private String getInternalPeerName(Address address) {
        Destination destination = ACDestinationAdapter.unwrap(address);
        return getInternalPeerName(destination);
    }
    
	private String getInternalPeerName(Destination destination) {
		LocalNode localNode = _cluster.getLocalNode();
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

	public void findRelevantSessionNames(int numPartitions, Collection[] resultSet) {
		throw new UnsupportedOperationException("NYI");
	}

	public void setClusterListener(ClusterListener listener) {
		_cluster.addClusterListener(new WADIClusterListenerAdapter(listener, _wadiCluster));
	}

    public org.codehaus.wadi.group.LocalPeer getLocalPeer() {
        return _localNode;
    }
    
    private String getNodeName(Node node) {
        return node==null?"<unknown>":(String)node.getState().get(_nodeNameKey);
    }
}
