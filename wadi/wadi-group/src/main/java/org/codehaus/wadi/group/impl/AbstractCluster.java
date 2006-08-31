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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.ClusterException;
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
    protected final Map _backendToPeer=new ConcurrentHashMap();

    protected final String _clusterName;
    protected final String _localPeerName;
    protected Peer _clusterPeer;
    protected LocalPeer _localPeer;
    protected ElectionStrategy _electionStrategy=new SeniorityElectionStrategy();

    private final List _clusterListeners = new ArrayList();
    private final Object membershipChangeLock = new Object();
    private Peer _coordinator;
    private boolean running;
    
    public AbstractCluster(String clusterName, String localPeerName) {
        _clusterName=clusterName;
        _localPeerName=localPeerName;
    }
    
    // 'Cluster' API
    public final void start() throws ClusterException {
    	running = true;

    	doStart();
    }

	public final void stop() throws ClusterException {
    	running = false;
    	doStop();
    }

    public Map getRemotePeers() {
        synchronized (_addressToPeer) {
            return new HashMap(_addressToPeer);
        }
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
        Set existing = new HashSet(_addressToPeer.values());
        listener.onListenerRegistration(this, existing, _coordinator);
    }

    public void removeClusterListener(ClusterListener listener) {
        boolean removed = false;
        synchronized (_clusterListeners) {
            removed = _clusterListeners.remove(listener);
        }
        if (!removed) {
            throw new IllegalArgumentException("[" + listener + "] was not registered.");
        }
    }
    
    protected void notifyMembershipChanged(Set joiners, Set leavers) {
    	synchronized (membershipChangeLock) {
        	electCoordinator();
        	
        	List snapshotListeners = snapshotListeners();
        	for (Iterator iter = snapshotListeners.iterator(); iter.hasNext();) {
    			ClusterListener listener = (ClusterListener) iter.next();
                listener.onMembershipChanged(this, joiners, leavers, _coordinator);
    		}
		}
    }

    public void notifyPeerUpdated(Peer peer) {
    	List snapshotListeners = snapshotListeners();
        ClusterEvent event=new ClusterEvent(this, peer, ClusterEvent.PEER_UPDATED);
    	for (Iterator iter = snapshotListeners.iterator(); iter.hasNext();) {
			ClusterListener listener = (ClusterListener) iter.next();
            listener.onPeerUpdated(event);
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

    protected abstract void doStart() throws ClusterException;

    protected abstract void doStop() throws ClusterException;

    protected void setFirstPeer() {
    	_coordinator = getLocalPeer();
    }
    
	private List snapshotListeners() {
		List snapshotListeners;
    	synchronized (_clusterListeners) {
			snapshotListeners = new ArrayList(_clusterListeners);
		}
		return snapshotListeners;
	}
	
    private void electCoordinator() {
        Peer coordinator = getLocalPeer();
        if (null != _electionStrategy && 0 < _addressToPeer.size()) {
            coordinator = _electionStrategy.doElection(this);
        }
        _coordinator = coordinator;
    }
}