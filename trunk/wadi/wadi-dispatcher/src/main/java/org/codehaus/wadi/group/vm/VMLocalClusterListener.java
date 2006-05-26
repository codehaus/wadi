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

    void notifyExistingPeersToPeer(Cluster cluster, Set existingPeers, Peer coordinator) {
        
        // Gianny...
        
        // I may have misunderstood what is going on... but I figure that this method needs to :
        
        // call the joining node's onMembershipChanged(), notifying it of existing peers
        // call each existing peer's onMembershipChanged(), notifying it of the joining peer
        
        // trouble is, I never seem to have exactly what I need, where I need it :-) and I keep getting 
        // confused between VM... and VMLocal....
        
        // maybe you can figure it out - TestInVMGroup is failing because I am not making membership notifications correctly.
        // It is working for JG and AC.
        // I hope to bring Tribes online soon as well.
        // JG and AC are now beginning to share code - see wadi-dispatcher/src/main/java/org/codehaus/wadi/group/impl/AbstractCluster.java
        
        // many thanks,
        
        // Jules
        
        
        if (false == localCluster.isRunning()) {
            return;
        }
        
        Peer joiner=peer;
        if (false == joiner.equals(peer)) {
            return;
        }
        
        if (peer.equals(joiner)) {
            delegate.onMembershipChanged(localCluster, existingPeers, Collections.EMPTY_SET, coordinator);
        } else {
            delegate.onMembershipChanged(localCluster, Collections.singleton(joiner), Collections.EMPTY_SET, coordinator);
        }
    }

    public void onMembershipChanged(Cluster cluster, Set joiners, Set leavers, Peer coordinator) {
        if (false == localCluster.isRunning()) {
            return;
        }

        if (joiners.contains(peer)) {
            Set newJoiners=new TreeSet();
            newJoiners.addAll(joiners);
            newJoiners.remove(peer);
            joiners=Collections.unmodifiableSet(newJoiners);
        }
        
        if (leavers.contains(peer)) {
            Set newLeavers=new TreeSet();
            newLeavers.addAll(leavers);
            newLeavers.remove(peer);
            leavers=Collections.unmodifiableSet(newLeavers);
        }
        
        delegate.onMembershipChanged(localCluster, joiners, leavers, coordinator);
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
