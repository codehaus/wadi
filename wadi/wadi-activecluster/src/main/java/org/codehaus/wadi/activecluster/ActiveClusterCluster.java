/**
 * Copyright 2006 The Apache Software Foundation
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

import java.util.Collections;
import java.util.Set;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;

import org.apache.activecluster.ClusterEvent;
import org.apache.activecluster.ClusterFactory;
import org.apache.activecluster.Node;
import org.apache.activecluster.impl.DefaultClusterFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.EndPoint;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.command.BootRemotePeer;
import org.codehaus.wadi.group.impl.AbstractCluster;

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * 
 * @version $Revision: 1603 $
 */
class ActiveClusterCluster extends AbstractCluster {
    
    protected ActiveMQConnectionFactory _connectionFactory;
    protected ClusterFactory _clusterFactory;
    protected org.apache.activecluster.Cluster _acCluster;
    protected Destination _clusterACDestination;
    protected Destination _localACDestination;
    protected Latch _startLatch;

    public ActiveClusterCluster(String clusterName, String localPeerName, String clusterUri, EndPoint endPoint, ActiveClusterDispatcher dispatcher) throws JMSException {
        super(clusterName, localPeerName, dispatcher);
        _clusterPeer = new ActiveClusterClusterPeer(this, clusterName);
        _localPeer = new ActiveClusterLocalPeer(this, localPeerName, endPoint);

        _connectionFactory = new ActiveMQConnectionFactory(clusterUri);
        DefaultClusterFactory tmp = new DefaultClusterFactory(_connectionFactory);
        tmp.setInactiveTime(_inactiveTime);
        _clusterFactory = tmp;

        _cluster.set(this);
    }

    public String toString() {
        return "ActiveClusterCluster [" + _localPeerName + "/" + _clusterName + "]";
    }

    public Address getAddress() {
        return (ActiveClusterPeer) _clusterPeer;
    }

    public LocalPeer getLocalPeer() {
        return _localPeer;
    }

    public Peer getPeerFromAddress(Address address) {
        return (ActiveClusterPeer) address;
    }
    
    private class NodeAddedAction implements Runnable {
        private final org.apache.activecluster.ClusterEvent event;
        
        public NodeAddedAction(ClusterEvent event) {
            this.event = event;
        }

        public void run() {
            Node node = event.getNode();
            EndPoint endPoint=(EndPoint)node.getState().get("EndPoint");
            _startLatch.release();
            _cluster.set(ActiveClusterCluster.this);
            ActiveClusterPeer remotePeer = new ActiveClusterPeer(ActiveClusterCluster.this, node.getName(), endPoint);
            remotePeer.init(node.getDestination());
            BootRemotePeer command = new BootRemotePeer(ActiveClusterCluster.this, remotePeer);
            Peer peer = command.getSerializedPeer();
            if (null == peer) {
                return;
            }
            synchronized (_addressToPeer) {
                _addressToPeer.put(peer, peer);
            }
            Set joiners = Collections.unmodifiableSet(Collections.singleton(peer));
            Set leavers = Collections.EMPTY_SET;
            notifyMembershipChanged(joiners, leavers);
        }
    }

    private class NodeFailedAction implements Runnable {
        private final org.apache.activecluster.ClusterEvent event;
        
        public NodeFailedAction(ClusterEvent event) {
            this.event = event;
        }

        public void run() {
            _cluster.set(ActiveClusterCluster.this);
            Node node = event.getNode();
            Peer failedPeer;
            Peer peer;
            synchronized (_backendKeyToPeer) {
                failedPeer = (Peer) _backendKeyToPeer.remove(node.getDestination());
            }
            if (failedPeer == null) {
                _log.warn("ActiveCluster issue - we have been notified of the loss of an unknown Peer - ignoring");
                return;
            }
            synchronized (_addressToPeer) {
                peer = (Peer) _addressToPeer.remove(failedPeer);
            }
            if (peer == null) {
                throw new AssertionError();
            }

            Set joiners = Collections.EMPTY_SET;
            Set leavers = Collections.unmodifiableSet(Collections.singleton(peer));
            notifyMembershipChanged(joiners, leavers);
        }
    }
    
    private void executeRunnable(Runnable runnable) {
        try {
            dispatcher.getExecutor().execute(runnable);
        } catch (InterruptedException e) {
            _log.error(e);
        }
    }

    class ACListener implements org.apache.activecluster.ClusterListener {

        public void onNodeAdd(org.apache.activecluster.ClusterEvent event) {
            NodeAddedAction action = new NodeAddedAction(event);
            executeRunnable(action);
        }

        public void onNodeUpdate(org.apache.activecluster.ClusterEvent event) {
        }

        public void onNodeRemoved(org.apache.activecluster.ClusterEvent event) {
            throw new UnsupportedOperationException("activecluster does not generate this event");
        }

        public void onNodeFailed(org.apache.activecluster.ClusterEvent event) {
            NodeFailedAction action = new NodeFailedAction(event);
            executeRunnable(action);
        }

        public void onCoordinatorChanged(org.apache.activecluster.ClusterEvent event) {
            // ignore - we are doing our coordinator management
        }

    };

    public synchronized void start() throws ClusterException {
        _startLatch = new Latch();
        try {
            _acCluster = _clusterFactory.createCluster(_clusterName);
            _acCluster.addClusterListener(new ACListener());
            
            MessageConsumer clusterConsumer = _acCluster.createConsumer(_acCluster.getDestination(), null, false);
            clusterConsumer.setMessageListener((ActiveClusterDispatcher) dispatcher);
            MessageConsumer nodeConsumer = _acCluster.createConsumer(_acCluster.getLocalNode().getDestination(), null, false);
            nodeConsumer.setMessageListener((ActiveClusterDispatcher) dispatcher);

            _clusterACDestination = _acCluster.getDestination();
            ((ActiveClusterClusterPeer) _clusterPeer).init(_clusterACDestination);
            _localACDestination = _acCluster.getLocalNode().getDestination();
            ((ActiveClusterLocalPeer) _localPeer).init(_localACDestination);
            _backendKeyToPeer.put(_localACDestination, _localPeer);

            _acCluster.start();
        } catch (JMSException e) {
            throw new ClusterException(e);
        }

        _log.info(_localPeerName + " - " + "connected to Cluster");

        boolean isFirstPeer;
        try {
            isFirstPeer = !_startLatch.attempt(_inactiveTime);
        } catch (InterruptedException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        }
        if (isFirstPeer) {
            setFirstPeer();
        }
    }

    public synchronized void stop() throws ClusterException {
        try {
            _acCluster.stop();
        } catch (JMSException e) {
            throw new ClusterException(e);
        }
        _startLatch = null;
    }

    public boolean equals(Object obj) {
        if (false == obj instanceof ActiveClusterCluster) {
            return false;
        }
        ActiveClusterCluster other = (ActiveClusterCluster) obj;
        return _acCluster.equals(other._acCluster);
    }

    public int hashCode() {
        return _acCluster.hashCode();
    }

    public org.apache.activecluster.Cluster getACCluster() {
        return _acCluster;
    }

    protected Object extractKeyFromPeerSerialization(Object backend) {
        ActiveClusterPeer remotePeer = (ActiveClusterPeer) backend;
        return remotePeer.getACDestination();
    }
    
    protected Peer createPeerFromPeerSerialization(Object backend) {
        ActiveClusterPeer remotePeer = (ActiveClusterPeer) backend;
        Destination acDestination = remotePeer.getACDestination();
        ActiveClusterPeer peer;
        if (acDestination.equals(_clusterACDestination)) {
            peer = (ActiveClusterPeer) _clusterPeer;
        } else {
            peer = new ActiveClusterRemotePeer(this, remotePeer);
        }
        return peer;
    }

    Destination getACDestination() {
        return _clusterACDestination;
    }

}
