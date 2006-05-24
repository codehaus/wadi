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
import org.apache.activecluster.ClusterListener;
import org.apache.activecluster.LocalNode;
import org.apache.activecluster.Node;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1603 $
 */
class WADIClusterListenerAdapter implements ClusterListener {
    private final org.codehaus.wadi.group.ClusterListener adaptee;
    private final Cluster cluster;
    
    public WADIClusterListenerAdapter(org.codehaus.wadi.group.ClusterListener adaptee, Cluster cluster) {
        this.adaptee = adaptee;
        this.cluster = cluster;
    }

    public void onNodeAdd(org.apache.activecluster.ClusterEvent event) {
        adaptee.onMembershipChanged(cluster, Collections.singleton(buildPeer(event.getNode())), Collections.EMPTY_SET);
    }

    public void onNodeUpdate(org.apache.activecluster.ClusterEvent event) {
        adaptee.onPeerUpdated(
            new ClusterEvent(cluster, 
                buildPeer(event.getNode()),
                ClusterEvent.PEER_UPDATED));
    }

    public void onNodeRemoved(org.apache.activecluster.ClusterEvent event) {
        throw new UnsupportedOperationException("activecluster does not generate this event");
    }

    public void onNodeFailed(org.apache.activecluster.ClusterEvent event) {
        adaptee.onMembershipChanged(cluster, Collections.EMPTY_SET, Collections.singleton(buildPeer(event.getNode())));
    }

    public void onCoordinatorChanged(org.apache.activecluster.ClusterEvent event) {
        adaptee.onCoordinatorChanged(
            new ClusterEvent(cluster, 
                buildPeer(event.getNode()),
                ClusterEvent.COORDINATOR_ELECTED));
    }
    
    public int hashCode() {
        return adaptee.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (false == obj instanceof WADIClusterListenerAdapter) {
            return false;
        }

        WADIClusterListenerAdapter other = (WADIClusterListenerAdapter) obj;
        return other.equals(adaptee);
    }
    
    private Peer buildPeer(Node node) {
        if (node instanceof LocalNode) {
            return new ACLocalNodeAdapter((LocalNode) node);
        }
        return new ACNodeAdapter(node);
    }
}
