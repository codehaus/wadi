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
package org.codehaus.wadi.replication;

import junit.framework.TestCase;

import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.gridstate.DispatcherConfig;
import org.codehaus.wadi.gridstate.activecluster.ActiveClusterDispatcher;
import org.codehaus.wadi.replication.common.NodeInfo;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.manager.basic.BasicReplicationManagerFactory;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.storage.ReplicaStorageFactory;
import org.codehaus.wadi.replication.storage.basic.BasicReplicaStorageFactory;
import org.codehaus.wadi.replication.strategy.RoundRobinBackingStrategyFactory;

public class SampleEndToEnd extends TestCase {
    private static final String CLUSTER_NAME = "OPENEJB_CLUSTER";
    private static final String CLUSTER_URI = "vm://clusterName?marshal=false&broker.persistent=false";
    private static final int TEMPO = 200;

    private NodeInfo nodeInfo1;
    private Dispatcher dispatcher1;
    private ReplicationManager manager1;
    private ReplicaStorage replicaStorage1;
    private NodeInfo nodeInfo2;
    private Dispatcher dispatcher2;
    private ReplicationManager manager2;
    private ReplicaStorage replicaStorage2;
    private NodeInfo nodeInfo3;
    private Dispatcher dispatcher3;
    private ReplicaStorage replicaStorage3;

    public void testSmoke() throws Exception {
        dispatcher1.start();
        manager1.start();
        replicaStorage1.start();

        Thread.sleep(TEMPO);

        dispatcher2.start();
        manager2.start();
        replicaStorage2.start();

        Thread.sleep(TEMPO);

        String key = "key1";
        String value = "value1";
        manager1.create(key, value);

        Thread.sleep(TEMPO);

        assertNotDefinedByStorage(key, replicaStorage1);
        assertDefineByStorage(key, replicaStorage2, new ReplicaInfo(nodeInfo1, new NodeInfo[] {nodeInfo2}, null));

        dispatcher3.start();
        replicaStorage3.start();

        Thread.sleep(TEMPO);

        assertNotDefinedByStorage(key, replicaStorage1);
        assertDefineByStorage(key, replicaStorage2, new ReplicaInfo(nodeInfo1, new NodeInfo[] {nodeInfo2, nodeInfo3}, null));
        assertDefineByStorage(key, replicaStorage3, new ReplicaInfo(nodeInfo1, new NodeInfo[] {nodeInfo2, nodeInfo3}, null));

        manager2.acquirePrimary(key);

        Thread.sleep(TEMPO);

        assertDefineByStorage(key, replicaStorage1, new ReplicaInfo(nodeInfo2, new NodeInfo[] {nodeInfo1, nodeInfo3}, null));
        assertNotDefinedByStorage(key, replicaStorage2);
        assertDefineByStorage(key, replicaStorage3, new ReplicaInfo(nodeInfo2, new NodeInfo[] {nodeInfo1, nodeInfo3}, null));

        manager1.acquirePrimary(key);

        Thread.sleep(TEMPO);

        assertNotDefinedByStorage(key, replicaStorage1);
        assertDefineByStorage(key, replicaStorage2, new ReplicaInfo(nodeInfo1, new NodeInfo[] {nodeInfo2, nodeInfo3}, null));
        assertDefineByStorage(key, replicaStorage3, new ReplicaInfo(nodeInfo1, new NodeInfo[] {nodeInfo2, nodeInfo3}, null));

        dispatcher3.start();
        replicaStorage3.stop();

        Thread.sleep(TEMPO);

        assertNotDefinedByStorage(key, replicaStorage1);
        assertDefineByStorage(key, replicaStorage2, new ReplicaInfo(nodeInfo1, new NodeInfo[] {nodeInfo2}, null));
        assertNotDefinedByStorage(key, replicaStorage3);

        replicaStorage2.stop();

        Thread.sleep(TEMPO);

        assertNotDefinedByStorage(key, replicaStorage1);
        assertNotDefinedByStorage(key, replicaStorage2);
        assertNotDefinedByStorage(key, replicaStorage3);

        replicaStorage3.start();

        Thread.sleep(TEMPO);

        assertNotDefinedByStorage(key, replicaStorage1);
        assertNotDefinedByStorage(key, replicaStorage2);
        assertDefineByStorage(key, replicaStorage3, new ReplicaInfo(nodeInfo1, new NodeInfo[] {nodeInfo3}, null));

        replicaStorage2.start();

        Thread.sleep(TEMPO);

        assertNotDefinedByStorage(key, replicaStorage1);
        assertDefineByStorage(key, replicaStorage2, new ReplicaInfo(nodeInfo1, new NodeInfo[] {nodeInfo3, nodeInfo2}, null));
        assertDefineByStorage(key, replicaStorage3, new ReplicaInfo(nodeInfo1, new NodeInfo[] {nodeInfo3, nodeInfo2}, null));
    }

    private void assertNotDefinedByStorage(String key, ReplicaStorage storage) {
        assertFalse(storage.storeReplicaInfo(key));
    }

    private void assertDefineByStorage(String key, ReplicaStorage storage, ReplicaInfo expected) {
        assertTrue(storage.storeReplicaInfo(key));

        ReplicaInfo replicaInfo = storage.retrieveReplicaInfo(key);

        assertEquals(expected.getPrimary(), replicaInfo.getPrimary());

        NodeInfo[] actualSecondaries = replicaInfo.getSecondaries();
        NodeInfo[] expectedSecondaries = expected.getSecondaries();
        assertEquals(expectedSecondaries.length, actualSecondaries.length);
        for (int i = 0; i < expectedSecondaries.length; i++) {
            assertEquals(expectedSecondaries[i], actualSecondaries[i]);
        }
    }

    protected void setUp() throws Exception {
        //setUpBroker();

        BasicReplicationManagerFactory managerFactory = new BasicReplicationManagerFactory();
        ReplicaStorageFactory storageFactory = new BasicReplicaStorageFactory();

        nodeInfo1 = new NodeInfo("node1");
        dispatcher1 = buildDispatcher(nodeInfo1);
        manager1 = managerFactory.factory(dispatcher1,
                nodeInfo1,
                storageFactory,
                new RoundRobinBackingStrategyFactory(2));
        replicaStorage1 = storageFactory.factory(dispatcher1, nodeInfo1);

        nodeInfo2 = new NodeInfo("node2");
        dispatcher2 = buildDispatcher(nodeInfo2);
        manager2 = managerFactory.factory(dispatcher2,
                nodeInfo2,
                storageFactory,
                new RoundRobinBackingStrategyFactory(2));
        replicaStorage2 = storageFactory.factory(dispatcher2, nodeInfo2);

        nodeInfo3 = new NodeInfo("node3");
        dispatcher3 = buildDispatcher(nodeInfo3);
        replicaStorage3 = storageFactory.factory(dispatcher3, nodeInfo3);
    }

//    private void setUpBroker() throws JMSException {
//        BrokerContainer broker = new BrokerContainerImpl(CLUSTER_NAME);
//        broker.addConnector(CLUSTER_URI);
//        broker.setPersistenceAdapter(new VMPersistenceAdapter());
//        broker.start();
//    }

    private Dispatcher buildDispatcher(NodeInfo nodeInfo) throws Exception {
        Dispatcher dispatcher =
            new ActiveClusterDispatcher(nodeInfo.getName(), CLUSTER_NAME, CLUSTER_URI, 5000L);
        dispatcher.init(new DispatcherConfig() {
            public String getContextPath() {
                return null;
            }
        });
        return dispatcher;
    }
}
