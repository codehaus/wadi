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
package org.codehaus.wadi.group.vm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1603 $
 */
class ClusterListenerSupport {
    private final Cluster cluster;
    private final List listeners = new ArrayList();
    private final Set nodes = new HashSet();

    public ClusterListenerSupport(Cluster cluster) {
        this.cluster = cluster;
    }

    public void addClusterListener(ClusterListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeClusterListener(ClusterListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void notifyAdd(Peer node) {
        Collection snapshotListeners;
        synchronized (listeners) {
            snapshotListeners = new ArrayList(listeners);
            nodes.add(node);
        }
        
        for (Iterator iter = snapshotListeners.iterator(); iter.hasNext();) {
            ClusterListener listener = (ClusterListener) iter.next();
            listener.onPeerAdded(new ClusterEvent(cluster, node, ClusterEvent.PEER_ADDED));
        }
    }

    public void notifyFailed(Peer node) {
        Collection snapshotListeners;
        synchronized (listeners) {
            snapshotListeners = new ArrayList(listeners);
            nodes.remove(node);
        }
        
        for (Iterator iter = snapshotListeners.iterator(); iter.hasNext();) {
            ClusterListener listener = (ClusterListener) iter.next();
            listener.onPeerFailed(new ClusterEvent(cluster, node, ClusterEvent.PEER_FAILED));
        }
    }

    public void notifyRemoved(Peer node) {
        Collection snapshotListeners;
        Set snapshotNodes;
        synchronized (listeners) {
            snapshotListeners = new ArrayList(listeners);
            snapshotNodes = new HashSet(nodes);
            nodes.remove(node);
        }
        
        for (Iterator iter = snapshotListeners.iterator(); iter.hasNext();) {
            ClusterListener listener = (ClusterListener) iter.next();
            listener.onPeerRemoved(new ClusterEvent(cluster, node, ClusterEvent.PEER_REMOVED));
        }
    }

    public void notifyUpdate(Peer node) {
        Collection snapshotListeners;
        synchronized (listeners) {
            snapshotListeners = new ArrayList(listeners);
        }
        
        ClusterEvent event = new ClusterEvent(cluster, node, ClusterEvent.PEER_UPDATED);
        for (Iterator iter = snapshotListeners.iterator(); iter.hasNext();) {
            ClusterListener listener = (ClusterListener) iter.next();
            listener.onPeerUpdated(event);
        }
    }

    public void notifyCoordinatorChanged(Peer node) {
        Collection snapshotListeners;
        synchronized (listeners) {
            snapshotListeners = new ArrayList(listeners);
        }
        
        for (Iterator iter = snapshotListeners.iterator(); iter.hasNext();) {
            ClusterListener listener = (ClusterListener) iter.next();
            listener.onCoordinatorChanged(new ClusterEvent(cluster, node, ClusterEvent.COORDINATOR_ELECTED));
        }
    }
}
