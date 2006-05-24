/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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
package org.codehaus.wadi.group.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.ElectionStrategy;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

public abstract class AbstractCluster implements Cluster {
    
    public static final ThreadLocal _cluster=new ThreadLocal();

    protected final Log _messageLog = LogFactory.getLog("org.codehaus.wadi.MESSAGES");
    protected final Log _log = LogFactory.getLog(getClass());
    protected final Map _addressToPeer = new HashMap();
    protected final long _inactiveTime = 5000;
    protected final List _clusterListeners = new ArrayList();
    protected final Map _backendToPeer=new ConcurrentHashMap();

    protected final String _clusterName;
    protected final String _localPeerName;
    protected Peer _clusterPeer;
    protected LocalPeer _localPeer;
    protected ElectionStrategy _electionStrategy=new SeniorityElectionStrategy();
    protected Peer _coordinator;

    public AbstractCluster(String clusterName, String localPeerName) {
        super();
        _clusterName=clusterName;
        _localPeerName=localPeerName;
    }
    
    // 'Cluster' API

    public Map getRemotePeers() {
        return Collections.unmodifiableMap(_addressToPeer); // could we cache this ? - TODO
    }

    public int getPeerCount() {
        synchronized (_addressToPeer) {
            return _addressToPeer.size()+1; // TODO - resolve - getNumNodes() returns N, but getRemoteNodes() returns N-1
        }
    }

    public boolean waitOnMembershipCount(int membershipCount, long timeout) throws InterruptedException {
        assert (membershipCount>0);
        membershipCount--; // remove ourselves from the equation...
        long expired=0;
        while ((getRemotePeers().size())!=membershipCount && expired<timeout) {
            Thread.sleep(1000);
            expired+=1000;
        }
        return (getRemotePeers().size())==membershipCount;
    }

    public long getInactiveTime() {
        return _inactiveTime;
    }

    public String getName() {
        return _clusterName;
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
    
    protected void notifyMembershipChanged(Set joiners, Set leavers) {
        int n=_clusterListeners.size();
        for (int i=0; i<n; i++) {
            ((ClusterListener)_clusterListeners.get(i)).onMembershipChanged(this, joiners, leavers);
        }
    }

    public void notifyPeerUpdated(Peer peer) {
        int n=_clusterListeners.size();
        if (n>0) {
            ClusterEvent event=new ClusterEvent(this, peer, ClusterEvent.PEER_UPDATED);
            for (int i=0; i<n; i++) {
                ((ClusterListener)_clusterListeners.get(i)).onPeerUpdated(event);
            }
        }
    }

    public void notifyCoordinatorChanged(Peer peer) {
        if (_coordinator==null || !_coordinator.equals(peer)) {
            _coordinator=peer;
            int n=_clusterListeners.size();
            if (n>0) {
                ClusterEvent event=new ClusterEvent(this, peer ,ClusterEvent.COORDINATOR_ELECTED);
                for (int i=0; i<n; i++) {
                    ((ClusterListener)_clusterListeners.get(i)).onCoordinatorChanged(event);
                }
            }
        }
    }
    
    /**
     * Look up a Peer from its JGroups Address using the ThreadLocal Cluster object.
     * 
     * @param backend The JGroups Address
     * @return The corresponding Peer
     */
    public static Peer get(Object backend) {
        AbstractCluster cluster=(AbstractCluster)_cluster.get();
        return cluster.getPeer(backend);
    }

    /**
     * Look up a Peer from its JGroups Address (possibly creating it in the process)
     * @param backend The JGroups Address
     * @return The corresponding Peer
     */
    public Peer getPeer(Object backend) {
        if (backend==null)
            return _clusterPeer;
        
        // TODO - optimise locking here later - we could use a Partitioned Lock Manager...
        Peer peer;
        synchronized (_backendToPeer) {
            peer=(Peer)_backendToPeer.get(backend);
            if (peer==null) {
                peer=create(backend);
                _backendToPeer.put(backend, peer);
            }
        }
        return peer;
    }
    
    protected abstract Peer create(Object backend);
    
    public void setElectionStrategy(ElectionStrategy strategy) {
        _electionStrategy=strategy;
    }

}