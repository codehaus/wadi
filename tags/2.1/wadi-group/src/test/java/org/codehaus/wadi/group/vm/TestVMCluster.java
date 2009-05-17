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

import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Peer;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1603 $
 */
public class TestVMCluster extends RMockTestCase {
    
    private VMBroker cluster;
    private VMDispatcher dispatcher1;
    private Cluster cluster1;
    private VMDispatcher dispatcher2;
    private Cluster cluster2;

    protected void setUp() throws Exception {
        cluster = new VMBroker("clusterName");
        
        dispatcher1 = new VMDispatcher(cluster, "node1", null);
        cluster1 = dispatcher1.getCluster();
        
        dispatcher2 = new VMDispatcher(cluster, "node2", null);
        cluster2 = dispatcher2.getCluster();
    }
    
    public void testExistingPeerSeeJoiningPeerAndViceVersa() throws Exception {
        ClusterListener listener1 = (ClusterListener) mock(ClusterListener.class);
        ClusterListener listener2 = (ClusterListener) mock(ClusterListener.class);
        
        listener1.onListenerRegistration(cluster1, Collections.EMPTY_SET);
        listener2.onListenerRegistration(cluster2, Collections.singleton((Peer) cluster1.getLocalPeer()));
        listener1.onMembershipChanged(cluster1, Collections.singleton((Peer) cluster2.getLocalPeer()), Collections.EMPTY_SET);
        
        startVerification();
        
        cluster1.addClusterListener(listener1);
        dispatcher1.start();
        
        cluster2.addClusterListener(listener2);
        dispatcher2.start();
    }
    
    public void testAddClusterListenerAfterStartSeeExistingPeer() throws Exception {
        ClusterListener listener2 = (ClusterListener) mock(ClusterListener.class);
        listener2.onListenerRegistration(dispatcher2.getCluster(), Collections.singleton((Peer) cluster1.getLocalPeer()));

        startVerification();
        
        dispatcher1.start();
        dispatcher2.start();
        cluster2.addClusterListener(listener2);
    }

}
