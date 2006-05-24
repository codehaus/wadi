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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jms.JMSException;

import org.apache.activecluster.ClusterFactory;
import org.apache.activecluster.Node;
import org.apache.activecluster.impl.DefaultClusterFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.AbstractCluster;
import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * 
 * @version $Revision: 1603 $
 */
class ActiveClusterCluster extends AbstractCluster {
    
    protected static final String _prefix="<"+Utils.basename(ActiveClusterCluster.class)+": ";
    protected static final String _suffix=">";

    protected ActiveMQConnectionFactory _connectionFactory;
    protected ClusterFactory _clusterFactory;
    protected org.apache.activecluster.Cluster _acCluster;
    protected javax.jms.Destination _clusterACDestination;
    protected javax.jms.Destination _localACDestination;

    protected Latch _startLatch;
    
    public ActiveClusterCluster(String clusterName, String localPeerName, String clusterUri) throws JMSException  {
        super(clusterName, localPeerName);
        _clusterPeer = new ActiveClusterClusterPeer(this);
        _localPeer = new ActiveClusterLocalPeer(this);
        ((ActiveClusterLocalPeer)_localPeer).setAttribute(Peer._peerNameKey, _localPeerName);

        _connectionFactory=new ActiveMQConnectionFactory(clusterUri);
        DefaultClusterFactory tmp=new DefaultClusterFactory(_connectionFactory);
        tmp.setInactiveTime(_inactiveTime);
        _clusterFactory=tmp;
        
        _cluster.set(this); // set ThreadLocal
    }
    
    // 'Object' API

    public String toString() {
        return _prefix+_localPeerName+"/"+_clusterName+_suffix;
    }
    
    public Address getAddress() {
        return (ActiveClusterPeer)_clusterPeer;
    }

    public LocalPeer getLocalPeer() {
        return _localPeer;
    }

    class ACListener implements org.apache.activecluster.ClusterListener {

        public void onNodeAdd(org.apache.activecluster.ClusterEvent event) {
            _cluster.set(ActiveClusterCluster.this);
            Node node=event.getNode();
            Peer peer=get(node.getDestination());
            Map state=node.getState();
            try {
                ((ActiveClusterRemotePeer)peer).setState(state);
            } catch (MessageExchangeException e) {
                _log.error("unexpected ActiveCluster problem", e);
            }
            _addressToPeer.put(peer,  peer);
            Set joiners=Collections.unmodifiableSet(Collections.singleton(peer));
            Set leavers=Collections.EMPTY_SET;
            notifyMembershipChanged(joiners, leavers); 
        }

        public void onNodeUpdate(org.apache.activecluster.ClusterEvent event) {
            _cluster.set(ActiveClusterCluster.this);
            Node node=event.getNode();
            Peer peer=get(node.getDestination());
            try {
                ((ActiveClusterPeer)peer).setState(node.getState());
                notifyPeerUpdated(peer);
            } catch (MessageExchangeException e) {
                _log.error("unexpected ActiveCluster problem", e);
            }
        }

        public void onNodeRemoved(org.apache.activecluster.ClusterEvent event) {
            throw new UnsupportedOperationException("activecluster does not generate this event");
        }

        public void onNodeFailed(org.apache.activecluster.ClusterEvent event) {
            _cluster.set(ActiveClusterCluster.this);
            Node node=event.getNode();
            Peer peer=(Peer)_addressToPeer.remove(_backendToPeer.get(node.getDestination()));
            Map state=node.getState();
            try {
                ((ActiveClusterRemotePeer)peer).setState(state);
            } catch (MessageExchangeException e) {
                _log.error("unexpected ActiveCluster problem", e);
            }
            Set joiners=Collections.EMPTY_SET;
            Set leavers=Collections.unmodifiableSet(Collections.singleton(peer));
            notifyMembershipChanged(joiners, leavers);
        }

        // called AFTER nodeAdd/Fail
        public void onCoordinatorChanged(org.apache.activecluster.ClusterEvent event) {
            _cluster.set(ActiveClusterCluster.this);
            notifyCoordinatorChanged(get(event.getNode().getDestination()));
            _startLatch.release();
        }

    };

    public void start() throws ClusterException {
        _startLatch=new Latch();
        try {
            _acCluster=_clusterFactory.createCluster(_clusterName);
            _acCluster.addClusterListener(new ACListener()); // attach our own listener, to keep us up to date...
            _acCluster.setElectionStrategy(new WADIElectionStrategyAdapter(_electionStrategy, this));
            _acCluster.getLocalNode().setState(_localPeer.getState());
            _acCluster.start();
        } catch (JMSException e) {
            throw new ClusterException(e);
        }
        
        _localACDestination = _acCluster.getDestination(); // too early ?
        long joinTime=System.currentTimeMillis();
        _log.info(_localPeerName+" - "+"connected to Cluster");
        _localACDestination=_acCluster.getLocalNode().getDestination();
        _clusterACDestination=_acCluster.getDestination();
        ((ActiveClusterClusterPeer)_clusterPeer).init(_clusterACDestination);
        ((ActiveClusterLocalPeer)_localPeer).init(_localACDestination);
        Map localState=_localPeer.getState();
        localState.put(Peer._peerNameKey, _localPeerName);
        localState.put(Peer._birthTimeKey, new Long(joinTime));
        _backendToPeer.put(_localACDestination, _localPeer);
        
        boolean needsNotification=false;
        try {
            needsNotification=!_startLatch.attempt(_inactiveTime);
        } catch (InterruptedException e) {
            _log.warn("unexpected interruption", e);
        }
        if (needsNotification) {
            // we must be the only member of the Cluster...
            for (Iterator i=_clusterListeners.iterator(); i.hasNext(); ) {
                ClusterListener listener=(ClusterListener)i.next();
                listener.onMembershipChanged(this, Collections.EMPTY_SET, Collections.EMPTY_SET);
                listener.onCoordinatorChanged(new ClusterEvent(this, _localPeer, ClusterEvent.COORDINATOR_ELECTED));
            }
        }
    }

    public void stop() throws ClusterException {
        try {
            _acCluster.stop();
        } catch (JMSException e) {
            throw new ClusterException(e);
        }
        _startLatch=null;
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

    protected Peer create(Object backend) {
        javax.jms.Destination acDestination=(javax.jms.Destination)backend;
        ActiveClusterPeer peer;
        if (acDestination.equals(_clusterACDestination)) {
            peer=(ActiveClusterPeer)_clusterPeer;
        } else {
            peer=new ActiveClusterRemotePeer(this, acDestination);
        }
        return peer;
    }

    javax.jms.Destination getACDestination() {
        return _clusterACDestination;
    }
    
}
