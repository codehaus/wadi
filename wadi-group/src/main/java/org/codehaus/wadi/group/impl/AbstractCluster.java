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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;

public abstract class AbstractCluster implements Cluster {
    public static final ThreadLocal<Cluster> clusterThreadLocal = new ThreadLocal<Cluster>();

    protected final Log log = LogFactory.getLog(getClass());
    protected final Map<Address, Peer> addressToPeer = new HashMap<Address, Peer>();
    protected final Map<Object, Peer> backendKeyToPeer = new ConcurrentHashMap<Object, Peer>();
    protected final String clusterName;
    protected final String localPeerName;
    protected final AbstractDispatcher dispatcher;
    protected Peer clusterPeer;
    protected LocalPeer localPeer;
    private final List<ClusterListener> clusterListeners = new CopyOnWriteArrayList<ClusterListener>();

    public AbstractCluster(String clusterName, String localPeerName, AbstractDispatcher dispatcher) {
        if (null == clusterName) {
            throw new IllegalArgumentException("clusterName is required");
        } else if (null == localPeerName) {
            throw new IllegalArgumentException("localPeerName is required");
        } else if (null == dispatcher) {
            throw new IllegalArgumentException("dispatcher is required");
        }
        this.clusterName = clusterName;
        this.localPeerName = localPeerName;
        this.dispatcher = dispatcher;
    }

    public String getClusterName() {
        return clusterName;
    }
    
    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public Map<Address, Peer> getRemotePeers() {
        synchronized (addressToPeer) {
            return new HashMap<Address, Peer>(addressToPeer);
        }
    }

    public int getPeerCount() {
        synchronized (addressToPeer) {
            return addressToPeer.size() + 1;
        }
    }

    public boolean waitOnMembershipCount(int membershipCount, long timeout) throws InterruptedException {
        assert (membershipCount > 0);
        // remove ourselves from the equation...
        membershipCount--; 
        long expired = 0;
        while ((getRemotePeers().size()) != membershipCount && expired < timeout) {
            Thread.sleep(1000);
            expired += 1000;
        }
        return (getRemotePeers().size()) == membershipCount;
    }

    public void addClusterListener(ClusterListener listener) {
        clusterListeners.add(listener);

        Set<Peer> existing;
        synchronized (addressToPeer) {
            existing = new HashSet<Peer>(addressToPeer.values());
        }
        listener.onListenerRegistration(this, existing);
    }

    public void removeClusterListener(ClusterListener listener) {
        boolean removed = clusterListeners.remove(listener);
        if (!removed) {
            throw new IllegalArgumentException("[" + listener + "] was not registered.");
        }
    }

    protected void notifyMembershipChanged(Set<Peer> joiners, Set<Peer> leavers) {
        for (ClusterListener listener : clusterListeners) {
            listener.onMembershipChanged(this, joiners, leavers);
        }
    }

    public static Peer get(Object serializedPeer) {
        AbstractCluster cluster = (AbstractCluster) clusterThreadLocal.get();
        return cluster.getPeer(serializedPeer);
    }

    public Peer getPeer(Object serializedPeer) {
        if (serializedPeer == null) {
            return clusterPeer;
        }

        Peer peer;
        synchronized (backendKeyToPeer) {
            Object backEndKey = extractKeyFromPeerSerialization(serializedPeer);
            peer = backendKeyToPeer.get(backEndKey);
            if (peer == null) {
                peer = createPeerFromPeerSerialization(serializedPeer);
                backendKeyToPeer.put(backEndKey, peer);
            }
        }
        return peer;
    }
    
    public Peer getPeerFromBackEndKey(Object backEndKey) {
        synchronized (backendKeyToPeer) {
            return backendKeyToPeer.get(backEndKey);
        }
    }

    protected abstract Peer createPeerFromPeerSerialization(Object backend);

    protected abstract Object extractKeyFromPeerSerialization(Object backend);

}