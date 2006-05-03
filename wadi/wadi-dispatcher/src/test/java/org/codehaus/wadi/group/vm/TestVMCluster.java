package org.codehaus.wadi.group.vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import junit.framework.TestCase;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.ClusterListener;

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
        assertSame(dispatcher2.getLocalPeer(), event.getPeer());
        assertSame(ClusterEvent.PEER_ADDED, event.getType());
        listener1.clearEvents();
        
        assertEquals(1, listener2.events.size());
        event = (ClusterEvent) listener2.events.get(0);
        assertSame(dispatcher1.getLocalPeer(), event.getPeer());
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
        assertSame(dispatcher1.getLocalPeer(), event.getPeer());
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
        assertSame(dispatcher1.getLocalPeer(), event.getPeer());
        assertSame(ClusterEvent.PEER_ADDED, event.getType());
        event = (ClusterEvent) listener2.events.get(1);
        assertSame(dispatcher1.getLocalPeer(), event.getPeer());
        assertSame(ClusterEvent.PEER_UPDATED, event.getType());
    }

    private static class MockClusterListener implements ClusterListener {
        public final List events = new ArrayList();

        public void clearEvents() {
            events.clear();
        }
        
        public void onPeerAdded(ClusterEvent event) {
            events.add(event);   
        }

        public void onPeerUpdated(ClusterEvent event) {
            events.add(event);   
        }

        public void onPeerRemoved(ClusterEvent event) {
            events.add(event);   
        }

        public void onPeerFailed(ClusterEvent event) {
            events.add(event);   
        }

        public void onCoordinatorChanged(ClusterEvent event) {
            events.add(event);   
        }
    }
}
