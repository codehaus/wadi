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

import junit.framework.TestCase;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.manager.basic.BasicReplicationManagerFactory;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.storage.ReplicaStorageFactory;
import org.codehaus.wadi.replication.storage.basic.BasicReplicaStorageFactory;
import org.codehaus.wadi.replication.strategy.RoundRobinBackingStrategyFactory;
import org.codehaus.wadi.servicespace.ServiceRegistry;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.basic.BasicServiceSpace;

public abstract class AbstractReplicationManagerTest extends TestCase {
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

    public void testSmoke() throws Exception {
        serviceSpace1.start();
        serviceSpace2.start();

        String key = "key1";
        String value = "value1";
        manager1.create(key, value);
        Thread.sleep(TEMPO);
        assertNotDefinedByStorage(key, replicaStorage1);
        assertDefinedByStorage(key, replicaStorage2, new ReplicaInfo(peer1, new Peer[] {peer2}));

        serviceSpace3.start();
        Thread.sleep(TEMPO);
        assertNotDefinedByStorage(key, replicaStorage1);
        assertDefinedByStorage(key, replicaStorage2, new ReplicaInfo(peer1, new Peer[] {peer2, peer3}));
        assertDefinedByStorage(key, replicaStorage3, new ReplicaInfo(peer1, new Peer[] {peer2, peer3}));

        manager2.acquirePrimary(key);
        assertDefinedByStorage(key, replicaStorage1, new ReplicaInfo(peer2, new Peer[] {peer1, peer3}));
        assertNotDefinedByStorage(key, replicaStorage2);
        assertDefinedByStorage(key, replicaStorage3, new ReplicaInfo(peer2, new Peer[] {peer1, peer3}));

        manager1.acquirePrimary(key);
        assertNotDefinedByStorage(key, replicaStorage1);
        assertDefinedByStorage(key, replicaStorage2, new ReplicaInfo(peer1, new Peer[] {peer2, peer3}));
        assertDefinedByStorage(key, replicaStorage3, new ReplicaInfo(peer1, new Peer[] {peer2, peer3}));

        serviceSpace3.stop();
        Thread.sleep(TEMPO);
        assertNotDefinedByStorage(key, replicaStorage1);
        assertDefinedByStorage(key, replicaStorage2, new ReplicaInfo(peer1, new Peer[] {peer2}));
        assertNotDefinedByStorage(key, replicaStorage3);

        serviceSpace2.stop();
        assertNotDefinedByStorage(key, replicaStorage1);
        assertNotDefinedByStorage(key, replicaStorage2);
        assertNotDefinedByStorage(key, replicaStorage3);

        serviceSpace3.start();
        Thread.sleep(TEMPO);
        assertNotDefinedByStorage(key, replicaStorage1);
        assertNotDefinedByStorage(key, replicaStorage2);
        assertDefinedByStorage(key, replicaStorage3, new ReplicaInfo(peer1, new Peer[] {peer3}));

        serviceSpace2.start();
        Thread.sleep(TEMPO);
        assertNotDefinedByStorage(key, replicaStorage1);
        assertDefinedByStorage(key, replicaStorage2, new ReplicaInfo(peer1, new Peer[] {peer3, peer2}));
        assertDefinedByStorage(key, replicaStorage3, new ReplicaInfo(peer1, new Peer[] {peer3, peer2}));
    }

    private void assertNotDefinedByStorage(String key, ReplicaStorage storage) {
        assertFalse(storage.storeReplicaInfo(key));
    }

    private void assertDefinedByStorage(String key, ReplicaStorage storage, ReplicaInfo expected) {
        assertTrue(storage.storeReplicaInfo(key));

        ReplicaInfo replicaInfo = storage.retrieveReplicaInfo(key);

        assertEquals(expected.getPrimary(), replicaInfo.getPrimary());

        Peer[] actualSecondaries = replicaInfo.getSecondaries();
        Peer[] expectedSecondaries = expected.getSecondaries();
        assertEquals(expectedSecondaries.length, actualSecondaries.length);
        for (int i = 0; i < expectedSecondaries.length; i++) {
            assertEquals(expectedSecondaries[i], actualSecondaries[i]);
        }
    }

    protected void setUp() throws Exception {
        BasicReplicationManagerFactory managerFactory = new BasicReplicationManagerFactory();
        ReplicaStorageFactory storageFactory = new BasicReplicaStorageFactory();
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
        
        return new BasicServiceSpace(new ServiceSpaceName(new URI("name")), dispatcher);
    }

    protected abstract Dispatcher createDispatcher(String clusterName, String nodeName, long timeout) throws Exception;
    
}
