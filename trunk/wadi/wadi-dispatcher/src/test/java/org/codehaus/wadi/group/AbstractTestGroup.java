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

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.impl.AbstractMsgDispatcher;
import org.codehaus.wadi.group.impl.RendezVousMsgDispatcher;
import EDU.oswego.cs.dl.util.concurrent.Latch;

public abstract class AbstractTestGroup extends TestCase {

    protected static Log _log = LogFactory.getLog(AbstractTestGroup.class);

    public AbstractTestGroup(String arg0) {
        super(arg0);
    }

    protected interface DispatcherFactory {
        Dispatcher create(String clusterName, String peerName, long inactiveTime) throws Exception;
    }

    public abstract DispatcherFactory getDispatcherFactory() throws Exception;

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

        assertEquals(1, listener0._numPeers);
        assertEquals(1, cluster0.getRemotePeers().size());
        assertEquals(1, listener1._numPeers);
        assertEquals(1, cluster1.getRemotePeers().size());

        cluster1.stop();
        assertTrue(cluster0.waitOnMembershipCount(1, 10000));

        assertEquals(0, listener0._numPeers);
        assertEquals(0, cluster0.getRemotePeers().size());

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
            assertSame(_local, content);
            assertSame(_local, target);
            assertSame(content, target);
            _latch.release();
        }
        
    }
    
    class SyncServiceEndpoint extends AbstractMsgDispatcher {
        
        protected final Address _local;
        
        SyncServiceEndpoint(Dispatcher dispatcher, Class type, Address local) {
            super(dispatcher, type);
            _local=local;
        }
        
        public void dispatch(Message om) throws Exception {
            Address content=(Address)om.getPayload();
            Address target=(Address)om.getAddress();
            assertSame(_local, content);
            assertSame(_local, target);
            assertSame(content, target);
            
            // reply - round tripping payload...
            _dispatcher.reply(om, content);
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
        ServiceEndpoint async=new AsyncServiceEndpoint(dispatcher1, Address.class, cluster1.getLocalPeer().getAddress(), latch);
        dispatcher1.register(async);
        Peer peer=(Peer)cluster0.getRemotePeers().values().iterator().next();
        dispatcher0.send(peer.getAddress(), peer.getAddress()); // red sends green its own address
        latch.acquire();
        dispatcher1.unregister(async, 10, 500); // what are these params for ?
        
        // sync - send a message red->green and reply green->red
        ServiceEndpoint rv=new RendezVousMsgDispatcher(dispatcher0, Address.class);
        dispatcher0.register(rv);
        ServiceEndpoint sync=new SyncServiceEndpoint(dispatcher1, Address.class, cluster1.getLocalPeer().getAddress());
        dispatcher1.register(sync);
        Peer peer2=(Peer)cluster0.getRemotePeers().values().iterator().next();
        Address target=peer2.getAddress();
        Message reply=dispatcher0.exchangeSend(target, target, 500000); // red sends green its own address and green sends it back
        Address payload=(Address)reply.getPayload();
        assertTrue(target==payload);
        dispatcher1.unregister(sync, 10, 500); // what are these params for ?
        dispatcher0.unregister(rv, 10, 5000); // what are these params for ?
        
        // etc...
        
        dispatcher1.stop();
        assertTrue(cluster0.waitOnMembershipCount(1, 10000));

        dispatcher0.stop();
    }
}
