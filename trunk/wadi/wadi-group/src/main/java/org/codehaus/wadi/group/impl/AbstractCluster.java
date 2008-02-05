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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;

public abstract class AbstractCluster implements Cluster {
    public static final ThreadLocal _cluster = new ThreadLocal();

    protected final Log _log = LogFactory.getLog(getClass());
    protected final Map _addressToPeer = new HashMap();
    protected final Map _backendKeyToPeer = new ConcurrentHashMap();
    protected final String _clusterName;
    protected final String _localPeerName;
    protected final AbstractDispatcher dispatcher;
    protected Peer _clusterPeer;
    protected LocalPeer _localPeer;
    private final List _clusterListeners = new CopyOnWriteArrayList();

    public AbstractCluster(String clusterName, String localPeerName, AbstractDispatcher dispatcher) {
        if (null == clusterName) {
            throw new IllegalArgumentException("clusterName is required");
        } else if (null == localPeerName) {
            throw new IllegalArgumentException("localPeerName is required");
        } else if (null == dispatcher) {
            throw new IllegalArgumentException("dispatcher is required");
        }
        _clusterName = clusterName;
        _localPeerName = localPeerName;
        this.dispatcher = dispatcher;
    }

    public String getClusterName() {
        return _clusterName;
    }
    
    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public Map getRemotePeers() {
        synchronized (_addressToPeer) {
            return new HashMap(_addressToPeer);
        }
    }

    public int getPeerCount() {
        synchronized (_addressToPeer) {
            return _addressToPeer.size() + 1;
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
        _clusterListeners.add(listener);

        Set existing;
        synchronized (_addressToPeer) {
            existing = new HashSet(_addressToPeer.values());
        }
        listener.onListenerRegistration(this, existing);
    }

    public void removeClusterListener(ClusterListener listener) {
        boolean removed = _clusterListeners.remove(listener);
        if (!removed) {
            throw new IllegalArgumentException("[" + listener + "] was not registered.");
        }
    }

    protected void notifyMembershipChanged(Set<Peer> joiners, Set<Peer> leavers) {
        for (Iterator iter = _clusterListeners.iterator(); iter.hasNext();) {
            ClusterListener listener = (ClusterListener) iter.next();
            listener.onMembershipChanged(this, joiners, leavers);
        }
    }

    public static Peer get(Object serializedPeer) {
        AbstractCluster cluster = (AbstractCluster) _cluster.get();
        return cluster.getPeer(serializedPeer);
    }

    public Peer getPeer(Object serializedPeer) {
        if (serializedPeer == null) {
            return _clusterPeer;
        }

        Peer peer;
        synchronized (_backendKeyToPeer) {
            Object backEndKey = extractKeyFromPeerSerialization(serializedPeer);
            peer = (Peer) _backendKeyToPeer.get(backEndKey);
            if (peer == null) {
                peer = createPeerFromPeerSerialization(serializedPeer);
                _backendKeyToPeer.put(backEndKey, peer);
            }
        }
        return peer;
    }
    
    public Peer getPeerFromBackEndKey(Object backEndKey) {
        synchronized (_backendKeyToPeer) {
            return (Peer) _backendKeyToPeer.get(backEndKey);
        }
    }

    protected abstract Peer createPeerFromPeerSerialization(Object backend);

    protected abstract Object extractKeyFromPeerSerialization(Object backend);

}