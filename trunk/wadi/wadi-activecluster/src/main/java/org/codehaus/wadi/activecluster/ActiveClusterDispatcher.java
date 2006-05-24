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
import org.apache.activecluster.LocalNode;
import org.apache.activecluster.Node;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.DispatcherConfig;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.AbstractCluster;
import org.codehaus.wadi.group.impl.AbstractDispatcher;

/**
 * A Dispatcher for ActiveCluster
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1788 $
 */
public class ActiveClusterDispatcher extends AbstractDispatcher {
    
    protected static final String _prefix="<"+Utils.basename(ActiveClusterDispatcher.class)+": ";
    protected static final String _suffix=">";
    
    
	protected org.apache.activecluster.Cluster _acCluster;
	protected MessageConsumer _clusterConsumer;
	protected MessageConsumer _nodeConsumer;

    protected final String _clusterName;
	protected final String _clusterUri;
    protected final String _localPeerName;
    protected final long _inactiveTime;
    private final WADIMessageListener _messageListener;

    private ActiveClusterCluster _cluster;
    protected LocalPeer _localPeer;
    
	public ActiveClusterDispatcher(String clusterName, String localPeerName, String clusterUri, long inactiveTime) throws Exception {
		super(inactiveTime);
        _clusterName=clusterName;
        _localPeerName=localPeerName;
		_clusterUri=clusterUri;
		_log=LogFactory.getLog(getClass()+"#"+localPeerName);
        _inactiveTime=inactiveTime;
        _cluster = new ActiveClusterCluster(_clusterName, _localPeerName, _clusterUri);
        _messageListener = new WADIMessageListener(_cluster, this);
        _localPeer = _cluster.getLocalPeer();
	}

    // 'java.lang.Object' API
    
    public String toString() {
        return _prefix+_cluster+_suffix;
    }
    
    // 5 calls
	public Cluster getCluster() {
		return _cluster;
	}

	//-----------------------------------------------------------------------------------------------
	// AbstractDispatcher overrides

	public void init(DispatcherConfig config) throws Exception {
		super.init(config);

	}

	//-----------------------------------------------------------------------------------------------
	// Dispatcher API

	public void start() throws MessageExchangeException {
	    try {
	        _cluster.start();
	    } catch (ClusterException e) {
	        throw new MessageExchangeException(e);
	    }
	    try {
	        boolean excludeSelf;
	        excludeSelf=false;
	        _acCluster =_cluster.getACCluster();
	        _clusterConsumer=_acCluster.createConsumer(_acCluster.getDestination(), null, excludeSelf);
	        _clusterConsumer.setMessageListener(_messageListener);
	        excludeSelf=false;
	        _nodeConsumer=_acCluster.createConsumer(_acCluster.getLocalNode().getDestination(), null, excludeSelf);
	        _nodeConsumer.setMessageListener(_messageListener);
	    } catch (JMSException e) {
	        _log.warn("unexpected ActiveCluster problem", e);
	    }

        Map distributedState = _localPeer.getState();
        distributedState.put(Peer._peerNameKey, _cluster.getLocalPeer().getName());
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
        } catch (ClusterException e) {
            throw new MessageExchangeException(e);
        } catch (InterruptedException e) {
            throw new MessageExchangeException(e);
        }
	}

	public Message createMessage() {
		try {
            return new ActiveClusterMessage(_acCluster.createObjectMessage());
        } catch (JMSException e) {
            throw new RuntimeJMSException(e);
        }
	}

	public void send(Address target, Message message) throws MessageExchangeException {
		if (_messageLog.isTraceEnabled()) {
            _messageLog.trace("outgoing: "+message.getPayload()+" {"+getInternalPeerName(message.getReplyTo())+"->"+getInternalPeerName(message.getAddress())+"} - "+message.getTargetCorrelationId()+"/"+message.getSourceCorrelationId()+" on "+Thread.currentThread().getName());
		}
		try {
            _acCluster.send(((ActiveClusterPeer)target).getACDestination(), ActiveClusterMessage.unwrap(message));
        } catch (JMSException e) {
            throw new MessageExchangeException(e);
        }
	}

	public void setDistributedState(Map state) throws MessageExchangeException {
	    _cluster.getLocalPeer().setState(state);
	}

    public Address getAddress(String nodeName) {
        if (_cluster.getLocalPeer().getName().equals(nodeName)) {
            return _localPeer.getAddress();
        }

        Map destToNode = _acCluster.getNodes();
        for (Iterator iter = destToNode.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            Destination destination = (Destination) entry.getKey();
            String name = getInternalPeerName(destination);
            if (name.equals(nodeName)) {
                return (ActiveClusterPeer)AbstractCluster.get(destination);
            }
        }

        throw new IllegalArgumentException("Node " + nodeName + " is undefined.");
    }

    public String getPeerName(Address address) {
        return getInternalPeerName(((ActiveClusterPeer)address).getACDestination());
    }

    private String getInternalPeerName(Address address) {
        Destination destination = ((ActiveClusterPeer)address).getACDestination();
        return getInternalPeerName(destination);
    }
    
	private String getInternalPeerName(Destination destination) {
		LocalNode localNode = _acCluster.getLocalNode();
		Destination localDestination=localNode.getDestination();

		if (destination==null) {
			return "<NULL-DESTINATION>";
		}

		if (destination.equals(localDestination))
			return getNodeName(localNode);

		Destination clusterDestination=_acCluster.getDestination();
		if (destination.equals(clusterDestination))
			return "cluster";

		Node node=null;
		if ((node=(Node)_acCluster.getNodes().get(destination))!=null)
			return getNodeName(node);

		return "<unknown>";
	}

	public void findRelevantSessionNames(int numPartitions, Collection[] resultSet) {
		throw new UnsupportedOperationException("NYI");
	}

	public void setClusterListener(ClusterListener listener) {
		_cluster.addClusterListener(listener);
	}

    private String getNodeName(Node node) {
        return node==null?"<unknown>":(String)node.getState().get(Peer._peerNameKey);
    }
}
