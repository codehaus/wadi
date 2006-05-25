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

import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.impl.AbstractMsgDispatcher;
import org.codehaus.wadi.group.impl.RendezVousMsgDispatcher;
import org.codehaus.wadi.group.impl.SeniorityElectionStrategy;
import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.WaitableInt;

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
        public WaitableInt _numCoordinators=new WaitableInt(0);
        public Peer _lastCoordinator=null;

        public void onPeerUpdated(ClusterEvent event) {
            _numUpdates++;
        }

        public void onMembershipChanged(Cluster cluster, Set joiners, Set leavers, Peer coordinator) {
            _lastCoordinator=coordinator;
            _numCoordinators.increment();
            _numPeers+=joiners.size();
            _numPeers-=leavers.size();
            _log.info(cluster.getLocalPeer().getName()+" - onMembershipChanged - joiners:"+joiners+", leavers:"+leavers+", coordinator:"+coordinator);
        }

    }

    public void testMembership() throws Exception {
        String clusterName="org.codehaus.wadi.cluster.TEST-"+System.currentTimeMillis();

        DispatcherConfig config=new DummyDispatcherConfig();
        
        Dispatcher dispatcher0=_dispatcherFactory.create(clusterName, "red", 5000);
        dispatcher0.init(config);
        
        MyClusterListener listener0=new MyClusterListener();
        Cluster cluster0=dispatcher0.getCluster();
        cluster0.setElectionStrategy(new SeniorityElectionStrategy());
        cluster0.addClusterListener(listener0);
        
        dispatcher0.start();
        assertTrue(listener0._numCoordinators.get()>=1); // we have received at least one MembershipChanged notification by the time start has finished
        assertTrue(listener0._lastCoordinator.equals(cluster0.getLocalPeer()));
        assertTrue(cluster0.waitOnMembershipCount(1, 10000));
        _log.info(cluster0);
        _log.info(dispatcher0);
        
        Dispatcher dispatcher1=_dispatcherFactory.create(clusterName, "green", 5000);
        dispatcher1.init(config);
        Cluster cluster1=dispatcher1.getCluster();
        cluster1.setElectionStrategy(new SeniorityElectionStrategy());
        MyClusterListener listener1=new MyClusterListener();
        cluster1.addClusterListener(listener1);

        dispatcher1.start();
        assertTrue(listener1._lastCoordinator.equals(cluster1.getRemotePeers().values().iterator().next())); // green knows red is coord
        assertTrue(listener1._numCoordinators.get()>=1);
        assertTrue(cluster0.waitOnMembershipCount(2, 10000));
        assertTrue(cluster1.waitOnMembershipCount(2, 10000));
        assertTrue(listener0._numCoordinators.get()>=2); // red has now held at least 2 coord elections
        assertTrue(listener0._lastCoordinator.equals(cluster0.getLocalPeer())); // red knowns red is coord
        _log.info(cluster1);
        _log.info(dispatcher1);

        assertEquals(1, listener0._numPeers);
        assertEquals(1, cluster0.getRemotePeers().size());
        assertEquals(1, listener1._numPeers);
        assertEquals(1, cluster1.getRemotePeers().size());
        
        Map peers0=cluster0.getRemotePeers();
        Peer remotePeer0=(Peer)peers0.values().iterator().next();
        assertTrue(peers0.get(remotePeer0.getAddress())==remotePeer0); // RemotePeer is an Address:Peer map
        Map peers1=cluster1.getRemotePeers();
        Peer remotePeer1=(Peer)peers1.values().iterator().next();
        assertTrue(peers1.get(remotePeer1.getAddress())==remotePeer1); // RemotePeer is an Address:Peer map
        
        dispatcher1.stop();
        cluster1.removeClusterListener(listener1);
        assertTrue(cluster0.waitOnMembershipCount(1, 10000));
        //assertTrue(listener0._numChanges.get()>=3); - how do we ensure that this has been called ?
        assertTrue(listener0._lastCoordinator.equals(cluster0.getLocalPeer()));

        assertEquals(0, listener0._numPeers);
        assertEquals(0, cluster0.getRemotePeers().size());

        dispatcher0.stop();
        cluster0.removeClusterListener(listener0);
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
            Address target=om.getAddress();
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
            Address target=om.getAddress();
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
        _log.info(cluster0);
        _log.info(dispatcher0);
        _log.info(cluster0.getRemotePeers());
        assertTrue(cluster0.waitOnMembershipCount(1, 10000));
        _log.info(cluster0.getRemotePeers());

        dispatcher1.start();
        _log.info(cluster1);
        _log.info(dispatcher1);
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
        Message reply=dispatcher0.exchangeSend(target, target, 5000); // red sends green its own address and green sends it back
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
