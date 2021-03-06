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

import org.codehaus.wadi.group.LocalPeer;

/**
 * 
 * @version $Revision: 1603 $
 */
class ClusterListenerSupport {
    private final VMBroker broker;
    private final List listeners = new ArrayList();

    public ClusterListenerSupport(VMBroker broker) {
        this.broker = broker;
    }

    public void addClusterListener(VMLocalClusterListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
        Set snapshotExistingPeers = snapshotExistingPeers();
        listener.notifyExistingPeers(snapshotExistingPeers);
    }

    public void removeClusterListener(VMLocalClusterListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public void notifyMembershipChanged(LocalPeer peer, boolean joining) {
        Collection snapshotListeners;
        synchronized (listeners) {
            snapshotListeners = new ArrayList(listeners);
        }
        
        if (joining) {
            notifyNewMembership(peer, snapshotListeners);
        } else {
            notifyRemovedMembership(peer, snapshotListeners);
        }
    }

    private void notifyNewMembership(LocalPeer peer, Collection listeners) {
        Set existingPeers = snapshotExistingPeers();
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            VMLocalClusterListener listener = (VMLocalClusterListener) iter.next();
            listener.notifyExistingPeersToPeer(existingPeers, peer);
        }
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            VMLocalClusterListener listener = (VMLocalClusterListener) iter.next();
            listener.notifyJoiningPeerToPeers(peer);
        }
    }

    private void notifyRemovedMembership(LocalPeer peer, Collection listeners) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            VMLocalClusterListener listener = (VMLocalClusterListener) iter.next();
            listener.notifyLeavingPeerToPeers(peer);
        }
    }

    private Set snapshotExistingPeers() {
        return new HashSet(broker.getPeers().values());
    }
}
