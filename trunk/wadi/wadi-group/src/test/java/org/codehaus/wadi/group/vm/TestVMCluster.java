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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1603 $
 */
public class TestVMCluster extends TestCase {
    
    public void testExistingPeerSeeJoiningPeerAndViceVersa() throws Exception {
        VMBroker cluster = new VMBroker("clusterName");
        
        VMDispatcher dispatcher1 = new VMDispatcher(cluster, "node1", 1000);
        MockClusterListener listener1 = new MockClusterListener();
        dispatcher1.getCluster().addClusterListener(listener1);
        dispatcher1.start();
        
        assertTrue(listener1.events.isEmpty());
        
        VMDispatcher dispatcher2 = new VMDispatcher(cluster, "node2", 1000);
        MockClusterListener listener2 = new MockClusterListener();
        dispatcher2.getCluster().addClusterListener(listener2);
        dispatcher2.start();
        
        assertEquals(1, listener1.events.size());
        ClusterEvent event = (ClusterEvent) listener1.events.get(0);
        assertSame(dispatcher2.getCluster().getLocalPeer(), event.getPeer());
        assertSame(ClusterEvent.PEER_ADDED, event.getType());
        listener1.clearEvents();
        
        assertEquals(1, listener2.events.size());
        event = (ClusterEvent) listener2.events.get(0);
        assertSame(dispatcher1.getCluster().getLocalPeer(), event.getPeer());
        assertSame(ClusterEvent.PEER_ADDED, event.getType());
        listener2.clearEvents();
    }
    
    public void testAddClusterListenerAfterStartSeeExistingPeer() throws Exception {
        VMBroker cluster = new VMBroker("clusterName");
        
        VMDispatcher dispatcher1 = new VMDispatcher(cluster, "node1", 1000);
        dispatcher1.start();
        
        VMDispatcher dispatcher2 = new VMDispatcher(cluster, "node2", 1000);
        dispatcher2.start();

        MockClusterListener listener2 = new MockClusterListener();
        dispatcher2.getCluster().addClusterListener(listener2);
        
        assertEquals(1, listener2.events.size());
        ClusterEvent event = (ClusterEvent) listener2.events.get(0);
        assertSame(dispatcher1.getCluster().getLocalPeer(), event.getPeer());
        assertSame(ClusterEvent.PEER_ADDED, event.getType());
    }

    private static class MockClusterListener implements ClusterListener {
        public final List events = new ArrayList();

        public void clearEvents() {
            events.clear();
        }

        public void onListenerRegistration(Cluster cluster, Set existing, Peer coordinator) {
        }
        
        public void onMembershipChanged(Cluster cluster, Set joiners, Set leavers, Peer coordinator) {
            for (Iterator iter = joiners.iterator(); iter.hasNext();) {
                Peer joiner = (Peer) iter.next();
                events.add(new ClusterEvent(cluster, joiner, ClusterEvent.PEER_ADDED));
            }
            for (Iterator iter = leavers.iterator(); iter.hasNext();) {
                Peer joiner = (Peer) iter.next();
                events.add(new ClusterEvent(cluster, joiner, ClusterEvent.PEER_REMOVED));
            }
        }

    }
}
