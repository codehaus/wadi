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
package org.codehaus.wadi.group;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.impl.AbstractMsgDispatcher;
import org.codehaus.wadi.group.vm.VMDispatcher;
import EDU.oswego.cs.dl.util.concurrent.Latch;
import junit.framework.TestCase;

public class TestGroup extends TestCase {

    protected static Log _log = LogFactory.getLog(TestGroup.class);

    public TestGroup(String arg0) {
        super(arg0);
    }

    protected interface DispatcherFactory {

        Dispatcher create(String clusterName, String peerName, long inactiveTime) throws Exception;

    }

    public DispatcherFactory getDispatcherFactory() throws Exception {
        return new DispatcherFactory() {public Dispatcher create(String clusterName, String peerName, long inactiveTime) throws Exception {return new VMDispatcher(clusterName, peerName, inactiveTime);}};
    }

    protected DispatcherFactory _dispatcherFactory;

    protected void setUp() throws Exception {
        super.setUp();
        _dispatcherFactory=getDispatcherFactory();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    class MyClusterListener implements ClusterListener {

        public int _numPeers=0;
        public int _numUpdates=0;
        public int _numChanges=0;

        public void onPeerAdded(ClusterEvent event) {
            _numPeers++;
            _log.info("onPeerAdded: "+_numPeers);
        }

        public void onPeerUpdated(ClusterEvent event) {
            _numUpdates++;
        }

        public void onPeerRemoved(ClusterEvent event) {
            _numPeers--;
            _log.info("onPeerAdded: "+_numPeers);
        }

        public void onPeerFailed(ClusterEvent event) {
            _numPeers--;
            _log.info("onPeerAdded: "+_numPeers);
        }

        public void onCoordinatorChanged(ClusterEvent event) {
            _numChanges++;
        }

    }

    public void donottestMembership() throws Exception {
        String clusterName="org.codehaus.wadi.cluster.TEST-"+System.currentTimeMillis();

        DispatcherConfig config=new DummyDispatcherConfig();
        
        Dispatcher dispatcher0=_dispatcherFactory.create(clusterName, "red", 5000);
        dispatcher0.init(config);
        
        MyClusterListener listener0=new MyClusterListener();
        Cluster cluster0=dispatcher0.getCluster();
        cluster0.addClusterListener(listener0);
        
        Dispatcher dispatcher1=_dispatcherFactory.create(clusterName, "green", 5000);
        dispatcher1.init(config);
        Cluster cluster1=dispatcher1.getCluster();
        MyClusterListener listener1=new MyClusterListener();
        cluster1.addClusterListener(listener1);

        cluster0.start();
        assertTrue(cluster0.waitOnMembershipCount(1, 10000));

        cluster1.start();
        assertTrue(cluster0.waitOnMembershipCount(2, 10000));
        assertTrue(cluster1.waitOnMembershipCount(2, 10000));

        assertTrue(listener0._numPeers==1);
        assertTrue(cluster0.getRemotePeers().size()==1);
        assertTrue(listener1._numPeers==1);
        assertTrue(cluster1.getRemotePeers().size()==1);

        cluster1.stop();
        assertTrue(cluster0.waitOnMembershipCount(1, 10000));

        assertTrue(listener0._numPeers==0);
        assertTrue(cluster0.getRemotePeers().size()==0);

        cluster0.stop();
    }

    class AsyncServiceEndpoint extends AbstractMsgDispatcher {
        
        protected final Address _local;
        protected final Latch _latch;
        
        AsyncServiceEndpoint(Dispatcher dispatcher, Class type, Address local, Latch latch) {
            super(dispatcher, type);
            _local=local;
            _latch=latch;
        }
        
        public void dispatch(Message om) throws Exception {
            Address content=(Address)om.getPayload();
            Address target=(Address)om.getAddress();
            // these tests will soon be done with '==' instead of 'equals()'...
            assertTrue(_local.equals(content));
            assertTrue(_local.equals(target));
            assertTrue(content.equals(target));
            _latch.release();
        }
        
    }
    
    public void testDispatcher() throws Exception {
        String clusterName="org.codehaus.wadi.cluster.TEST-"+System.currentTimeMillis();

        DispatcherConfig config=new DummyDispatcherConfig();
        
        Dispatcher dispatcher0=_dispatcherFactory.create(clusterName, "red", 5000);
        Dispatcher dispatcher1=_dispatcherFactory.create(clusterName, "green", 5000);

        dispatcher0.init(config);
        dispatcher1.init(config);

        Cluster cluster0=dispatcher0.getCluster();
        Cluster cluster1=dispatcher1.getCluster();

        dispatcher0.start();
        assertTrue(cluster0.waitOnMembershipCount(1, 10000));

        dispatcher1.start();
        assertTrue(cluster0.waitOnMembershipCount(2, 10000));
        assertTrue(cluster1.waitOnMembershipCount(2, 10000));

        // async - send a message (content=green's Address) red->green - confirm equality of local, target and content Addresses
        Latch latch=new Latch();
        dispatcher1.register(new AsyncServiceEndpoint(dispatcher1, Address.class, cluster1.getLocalPeer().getAddress(), latch));
        Peer peer=(Peer)cluster0.getRemotePeers().values().iterator().next();
        dispatcher0.send(peer.getAddress(), cluster1.getLocalPeer().getAddress()); // red sends green its address
        latch.acquire();
        
        // sync - send a message red->green and reply green->red
        
        // etc...
        
        dispatcher1.stop();
        assertTrue(cluster0.waitOnMembershipCount(1, 10000));

        dispatcher0.stop();
    }
}
