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
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.ClusterListener;

/**
 * 
 * @version $Revision: 1603 $
 */
public class TestVMCluster extends TestCase {
    
    public void testExistingPeerSeeJoiningPeerAndViceVersa() throws Exception {
        VMCluster cluster = new VMCluster("clusterName");
        
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
        VMCluster cluster = new VMCluster("clusterName");
        
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

    public void testPeerUpdatedIsNotPropagatedToStoppedPeer() throws Exception {
        VMCluster cluster = new VMCluster("clusterName");
        
        VMDispatcher dispatcher1 = new VMDispatcher(cluster, "node1", 1000);
        dispatcher1.start();

        VMDispatcher dispatcher2 = new VMDispatcher(cluster, "node2", 1000);
        MockClusterListener listener2 = new MockClusterListener();
        dispatcher2.getCluster().addClusterListener(listener2);

        HashMap state = new HashMap();
        dispatcher1.setDistributedState(state);
        assertTrue(listener2.events.isEmpty());

        dispatcher2.start();

        dispatcher1.setDistributedState(state);
        
        assertEquals(2, listener2.events.size());
        ClusterEvent event = (ClusterEvent) listener2.events.get(0);
        assertSame(dispatcher1.getCluster().getLocalPeer(), event.getPeer());
        assertSame(ClusterEvent.PEER_ADDED, event.getType());
        event = (ClusterEvent) listener2.events.get(1);
        assertSame(dispatcher1.getCluster().getLocalPeer(), event.getPeer());
        assertSame(ClusterEvent.PEER_UPDATED, event.getType());
    }

    private static class MockClusterListener implements ClusterListener {
        public final List events = new ArrayList();

        public void clearEvents() {
            events.clear();
        }
        
<<<<<<< .mine
        public void onMembershipChanged(Cluster cluster, Set joiners, Set leavers) {
            // events.add(event); - FIXME   
=======
        public void onMembershipChanged(Cluster cluster, Set joiners, Set leavers) {
            events.add(event);   
>>>>>>> .r1819
        }

        public void onPeerUpdated(ClusterEvent event) {
            events.add(event);   
        }
        public void onCoordinatorChanged(ClusterEvent event) {
            events.add(event);   
        }
    }
}
