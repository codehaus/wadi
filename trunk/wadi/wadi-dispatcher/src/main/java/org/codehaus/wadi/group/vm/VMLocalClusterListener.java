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

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1603 $
 */
public class VMLocalClusterListener implements ClusterListener {
    private final VMLocalCluster localCluster;
    private final ClusterListener delegate;
    private final Peer peer;

    public VMLocalClusterListener(VMLocalCluster localCluster, ClusterListener delegate, Peer peer) {
        this.localCluster = localCluster;
        this.delegate = delegate;
        this.peer = peer;
    }

    void notifyExistingPeersToPeer(Cluster cluster, Set existingPeers) {
        notifyExistingPeersToPeer(peer, cluster, existingPeers);
    }

    void notifyExistingPeersToPeer(Peer target, Cluster cluster, Set existingPeers) {
        if (false == localCluster.isRunning()) {
            return;
        }
        
        if (false == target.equals(peer)) {
            return;
        }
        Set joiners=new TreeSet(existingPeers);
        joiners.remove(peer);
        Set leavers=Collections.EMPTY_SET;
        delegate.onMembershipChanged(cluster, joiners, leavers);
    }
    
    public void onCoordinatorChanged(ClusterEvent event) {
        if (false == localCluster.isRunning()) {
            return;
        }

        delegate.onCoordinatorChanged(event);
    }

    public void onMembershipChanged(Cluster cluster, Set joiners, Set leavers) {
        if (false == localCluster.isRunning()) {
            return;
        }

        if (joiners.contains(peer)) {
            joiners=new TreeSet(joiners);
            joiners.remove(peer);
            joiners=Collections.unmodifiableSet(joiners);
        }
        
        if (leavers.contains(peer)) {
            leavers=new TreeSet(leavers);
            leavers.remove(peer);
            leavers=Collections.unmodifiableSet(leavers);
        }
        
        if (joiners.size()==0 && leavers.size()==0) {
            return;
        }
        
        delegate.onMembershipChanged(cluster, joiners, leavers);
    }

    public void onPeerUpdated(ClusterEvent event) {
        if (false == localCluster.isRunning()) {
            return;
        }

        if (event.getPeer().equals(peer)) {
            return;
        }
        delegate.onPeerUpdated(event);
    }
    
    public boolean equals(Object obj) {
        if (false == obj instanceof VMLocalClusterListener) {
            return false;
        }
        
        VMLocalClusterListener other = (VMLocalClusterListener) obj;
        return peer.equals(other.peer) && delegate.equals(other.delegate);
    }
    
    public int hashCode() {
        return peer.hashCode() * delegate.hashCode();
    }
}
