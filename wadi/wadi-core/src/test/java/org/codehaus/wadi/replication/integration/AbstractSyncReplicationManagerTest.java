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
package org.codehaus.wadi.replication.integration;

import java.net.URI;
import java.util.Iterator;

import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.reflect.base.DeclaredMemberFilter;
import org.codehaus.wadi.core.reflect.jdk.JDKClassIndexerRegistry;
import org.codehaus.wadi.core.session.BasicValueHelperRegistry;
import org.codehaus.wadi.core.session.DistributableAttributesFactory;
import org.codehaus.wadi.core.session.DistributableSessionFactory;
import org.codehaus.wadi.core.session.DistributableValueFactory;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.core.session.SessionFactory;
import org.codehaus.wadi.core.util.SimpleStreamer;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.common.ReplicaStorageInfo;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.manager.basic.ObjectStateHandler;
import org.codehaus.wadi.replication.manager.basic.SessionStateHandler;
import org.codehaus.wadi.replication.manager.basic.SyncReplicationManagerFactory;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.storage.ReplicaStorageFactory;
import org.codehaus.wadi.replication.storage.memory.SyncMemoryReplicaStorageFactory;
import org.codehaus.wadi.replication.strategy.RoundRobinBackingStrategyFactory;
import org.codehaus.wadi.servicespace.ServiceRegistry;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.basic.BasicServiceSpace;

import com.agical.rmock.extension.junit.RMockTestCase;

public abstract class AbstractSyncReplicationManagerTest extends RMockTestCase {
    private static final String CLUSTER_NAME = "CLUSTER";
    private static final long TEMPO = 1000;

    private BasicServiceSpace serviceSpace1;
    private Peer peer1;
    private Dispatcher dispatcher1;
    private ReplicationManager manager1;
    private ReplicaStorage replicaStorage1;

    private BasicServiceSpace serviceSpace2;
    private Peer peer2;
    private Dispatcher dispatcher2;
    private ReplicationManager manager2;
    private ReplicaStorage replicaStorage2;
    
    private BasicServiceSpace serviceSpace3;
    private Peer peer3;
    private Dispatcher dispatcher3;
    private ReplicaStorage replicaStorage3;

    private SessionFactory sessionFactory;
    
    public void testSmoke() throws Exception {
        serviceSpace1.start();
        Thread.sleep(1000);
        serviceSpace2.start();

        String key = "key1";
        Session session = sessionFactory.create();
        session.init(1, 2, 3, key);
        session.addState("key", "value");
        manager1.create(key, session);
        Thread.sleep(TEMPO);
        assertNotDefinedByStorage(key, replicaStorage1);
        assertDefinedByStorage(key, 0, replicaStorage2, new ReplicaInfo(peer1, new Peer[] {peer2}, session));

        serviceSpace3.start();
        Thread.sleep(TEMPO);
        assertNotDefinedByStorage(key, replicaStorage1);
        assertDefinedByStorage(key, 1, replicaStorage2, new ReplicaInfo(peer1, new Peer[] {peer2, peer3}, session));
        assertDefinedByStorage(key, 1, replicaStorage3, new ReplicaInfo(peer1, new Peer[] {peer2, peer3}, session));

        manager2.retrieveReplica(key);
        assertDefinedByStorage(key, 2, replicaStorage1, new ReplicaInfo(peer2, new Peer[] {peer1, peer3}, session));
        assertNotDefinedByStorage(key, replicaStorage2);
        assertDefinedByStorage(key, 2, replicaStorage3, new ReplicaInfo(peer2, new Peer[] {peer1, peer3}, session));

        manager1.retrieveReplica(key);
        assertNotDefinedByStorage(key, replicaStorage1);
        assertDefinedByStorage(key, 3, replicaStorage2, new ReplicaInfo(peer1, new Peer[] {peer2, peer3}, session));
        assertDefinedByStorage(key, 3, replicaStorage3, new ReplicaInfo(peer1, new Peer[] {peer2, peer3}, session));

        serviceSpace3.stop();
        Thread.sleep(TEMPO);
        assertNotDefinedByStorage(key, replicaStorage1);
        assertDefinedByStorage(key, 4, replicaStorage2, new ReplicaInfo(peer1, new Peer[] {peer2}, session));
        assertNotDefinedByStorage(key, replicaStorage3);

        serviceSpace2.stop();
        assertNotDefinedByStorage(key, replicaStorage1);
        assertNotDefinedByStorage(key, replicaStorage2);
        assertNotDefinedByStorage(key, replicaStorage3);

        serviceSpace3.start();
        Thread.sleep(TEMPO);
        assertNotDefinedByStorage(key, replicaStorage1);
        assertNotDefinedByStorage(key, replicaStorage2);
        assertDefinedByStorage(key, 5, replicaStorage3, new ReplicaInfo(peer1, new Peer[] {peer3}, session));

        serviceSpace2.start();
        Thread.sleep(TEMPO);
        assertNotDefinedByStorage(key, replicaStorage1);
        assertDefinedByStorage(key, 6, replicaStorage2, new ReplicaInfo(peer1, new Peer[] {peer3, peer2}, session));
        assertDefinedByStorage(key, 6, replicaStorage3, new ReplicaInfo(peer1, new Peer[] {peer3, peer2}, session));
    }

    private void assertNotDefinedByStorage(String key, ReplicaStorage storage) {
        assertFalse(storage.storeReplicaInfo(key));
    }

    private void assertDefinedByStorage(String key, int version, ReplicaStorage storage, ReplicaInfo expected) {
        assertTrue(storage.storeReplicaInfo(key));

        ReplicaStorageInfo storageInfo = storage.retrieveReplicaStorageInfo(key);
        ReplicaInfo replicaInfo = storageInfo.getReplicaInfo();

        assertEquals(version, storageInfo.getVersion());
        assertEquals(expected.getPrimary(), replicaInfo.getPrimary());

        Peer[] actualSecondaries = replicaInfo.getSecondaries();
        Peer[] expectedSecondaries = expected.getSecondaries();
        assertEquals(expectedSecondaries.length, actualSecondaries.length);
        for (int i = 0; i < expectedSecondaries.length; i++) {
            assertEquals(expectedSecondaries[i], actualSecondaries[i]);
        }
        
        Session expectedSession = (Session) expected.getPayload();
        Session actualSession = (Session) replicaInfo.getPayload();
        for (Iterator iterator = expectedSession.getState().keySet().iterator(); iterator.hasNext();) {
            Object stateKey = iterator.next();
            assertEquals(expectedSession.getState(stateKey), actualSession.getState(stateKey));
        }
    }

    protected void setUp() throws Exception {
        Streamer streamer = new SimpleStreamer();
        sessionFactory = new DistributableSessionFactory(
            new DistributableAttributesFactory(new DistributableValueFactory(new BasicValueHelperRegistry())),
            streamer);
        Manager manager = (Manager) mock(Manager.class);
        sessionFactory.setManager(manager);
        
        ObjectStateHandler objectStateManager = new SessionStateHandler(streamer);
        objectStateManager.setObjectFactory(sessionFactory);
        
        ReplicaStorage replicaStorage = (ReplicaStorage) mock(ReplicaStorage.class);
        
        SyncReplicationManagerFactory managerFactory = new SyncReplicationManagerFactory(objectStateManager, replicaStorage);
        ReplicaStorageFactory storageFactory = new SyncMemoryReplicaStorageFactory(objectStateManager);
        RoundRobinBackingStrategyFactory backingStrategy = new RoundRobinBackingStrategyFactory(2);

        serviceSpace1 = buildServiceSpace("peer1");
        dispatcher1 = serviceSpace1.getDispatcher();
        peer1 = dispatcher1.getCluster().getLocalPeer();
        manager1 = managerFactory.factory(serviceSpace1, backingStrategy);
        replicaStorage1 = storageFactory.factory(serviceSpace1);
        ServiceRegistry serviceRegistry = serviceSpace1.getServiceRegistry();
        serviceRegistry.register(ReplicationManager.NAME, manager1);
        serviceRegistry.register(ReplicaStorage.NAME, replicaStorage1);
        
        serviceSpace2 = buildServiceSpace("peer2");
        dispatcher2 = serviceSpace2.getDispatcher();
        peer2 = dispatcher2.getCluster().getLocalPeer();
        manager2 = managerFactory.factory(serviceSpace2, backingStrategy);
        replicaStorage2 = storageFactory.factory(serviceSpace2);
        serviceRegistry = serviceSpace2.getServiceRegistry();
        serviceRegistry.register(ReplicationManager.NAME, manager2);
        serviceRegistry.register(ReplicaStorage.NAME, replicaStorage2);
        
        serviceSpace3 = buildServiceSpace("peer3");
        dispatcher3 = serviceSpace3.getDispatcher();
        peer3 = dispatcher3.getCluster().getLocalPeer();
        replicaStorage3 = storageFactory.factory(serviceSpace3);
        serviceRegistry = serviceSpace3.getServiceRegistry();
        serviceRegistry.register(ReplicaStorage.NAME, replicaStorage3);
    }

    private BasicServiceSpace buildServiceSpace(String nodeName) throws Exception {
        Dispatcher dispatcher = createDispatcher(CLUSTER_NAME, nodeName, 5000L);
        dispatcher.start();
        
        return new BasicServiceSpace(new ServiceSpaceName(new URI("name")),
            dispatcher,
            new JDKClassIndexerRegistry(new DeclaredMemberFilter()));
    }

    protected abstract Dispatcher createDispatcher(String clusterName, String nodeName, long timeout) throws Exception;
    
}
