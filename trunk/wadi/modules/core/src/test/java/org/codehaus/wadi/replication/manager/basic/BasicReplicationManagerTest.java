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
package org.codehaus.wadi.replication.manager.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.codehaus.wadi.replication.common.NodeInfo;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.manager.ReplicationManagerStubFactory;
import org.codehaus.wadi.replication.manager.remoting.ReplicationManagerExporter;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.storage.ReplicaStorageStubFactory;
import org.codehaus.wadi.replication.storage.remoting.ReplicaStorageListener;
import org.codehaus.wadi.replication.storage.remoting.ReplicaStorageMonitor;
import org.codehaus.wadi.replication.strategy.BackingStrategy;

public class BasicReplicationManagerTest extends TestCase {
    private BasicReplicationManager manager;
    private MockBackingStrategy backingStrategy;
    private MockReplicationManagerFactory rmf;
    private MockReplicaStorageStubFactory rsf;
    private MockReplicaStorageMonitor rsm;
    private NodeInfo node1;
    private NodeInfo node2;
    private NodeInfo node3;
    private NodeInfo node4;
    private MockReplicaStorage replicaStorage;
    private MockReplicationManagerExporter rme;
    private MockReplicaStorage rs;

    public void testCreate() {
        Object key = new Object();
        Object payload = new Object();
        
        ReplicaInfo expectedReplicaInfo = fillWithKeyAndObject(key, payload);
        
        assertTrue(manager.managePrimary(key));
        ReplicaInfo actualReplicaInfo = manager.retrieveReplicaInfo(key);
        
        assertReplicaInfo(expectedReplicaInfo, actualReplicaInfo);
    }

    public void testUpdate() {
        Object key = new Object();
        Object payload = new Object();
        
        ReplicaInfo expectedReplicaInfo = fillWithKeyAndObject(key, payload);
        
        payload = new Object();

        rsf.index = 0;
        
        expectedReplicaInfo = new ReplicaInfo(null, null, payload);
        replicaStorage.expectedUpdateReplicaInfo = expectedReplicaInfo;
        manager.update(key, payload);
        
        ReplicaInfo actualReplicaInfo = manager.retrieveReplicaInfo(key);
        assertReplicaInfo(
                new ReplicaInfo(node1, backingStrategy.resultElectSecondaries, payload),
                actualReplicaInfo);
    }

    public void testDestroy() {
        Object key = new Object();
        Object payload = new Object();
        
        fillWithKeyAndObject(key, payload);

        rsf.index = 0;
        
        manager.destroy(key);
        assertFalse(manager.managePrimary(key));
    }

    public void testAcquirePrimary() {
        Object key = new Object();
        Object payload = new Object();
        
        MockReplicationManager rm = new MockReplicationManager();
        rm.expectedKey = key;
        NodeInfo[] oldSecondaries = new NodeInfo[] {node3, node4};
        rm.resultReleasePrimary = new ReplicaInfo(node2, oldSecondaries, payload);
        rmf.resultBuildStub = rm;
        
        backingStrategy.expectedKey = key;
        backingStrategy.expectedReElectSecondariesPrimary = node2;
        backingStrategy.expectedReElectSecondariesSecondaries = oldSecondaries;
        backingStrategy.resultReElectSecondaries = new NodeInfo[] {node2, node3}; 
        
        rsf.index = 0;
        
        NodeInfo[] createNode = new NodeInfo[] {node2};
        MockReplicaStorage createStub = new MockReplicaStorage();
        createStub.expectedKey = key;
        createStub.expectedCreateReplicaInfo = new ReplicaInfo(node1,
                backingStrategy.resultReElectSecondaries,
                payload);
        
        NodeInfo[] updateNode = new NodeInfo[] {node3};
        MockReplicaStorage updateStub = new MockReplicaStorage();
        updateStub.expectedKey = key;
        updateStub.expectedUpdateReplicaInfo = new ReplicaInfo(node1,
                backingStrategy.resultReElectSecondaries,
                null);
        
        NodeInfo[] destroyNode = new NodeInfo[] {node4};
        MockReplicaStorage destroyStub = new MockReplicaStorage();
        destroyStub.expectedKey = key;
        
        rsf.expectedNodesList = Arrays.asList(new Object[] {createNode, updateNode, destroyNode});
        rsf.resultBuildStubWithNodesList = Arrays.asList(new Object[] {createStub, updateStub, destroyStub});
        
        manager.acquirePrimary(key);
        
        ReplicaInfo actualReplicaInfo = manager.retrieveReplicaInfo(key);
        assertReplicaInfo(
                new ReplicaInfo(node1, backingStrategy.resultReElectSecondaries, payload),
                actualReplicaInfo);
    }

    public void testUpdateBackingStrategy() throws Exception {
        manager.start();
        rsm.listener.fireJoin(node1);
        assertEquals(0, backingStrategy.secondaries.size());
        rsm.listener.fireJoin(node2);
        assertEquals(1, backingStrategy.secondaries.size());
        assertSame(node2, backingStrategy.secondaries.get(0));
        rsm.listener.fireLeave(node2);
        assertEquals(0, backingStrategy.secondaries.size());
    }
    
    protected void setUp() throws Exception {
        backingStrategy = new MockBackingStrategy();
        rmf = new MockReplicationManagerFactory();
        rsf = new MockReplicaStorageStubFactory();
        rsm = new MockReplicaStorageMonitor();
        rs = new MockReplicaStorage();
        rme = new MockReplicationManagerExporter();
        node1 = new NodeInfo("node1");
        node2 = new NodeInfo("node2");
        node3 = new NodeInfo("node3");
        node4 = new NodeInfo("node4");
        manager = new BasicReplicationManager(backingStrategy,
                rmf,
                rsf,
                rsm,
                rs,
                rme,
                node1);
    }
    
    private ReplicaInfo fillWithKeyAndObject(Object key, Object payload) {
        backingStrategy.expectedKey = key;
        backingStrategy.resultElectSecondaries = new NodeInfo[] {node2, node3};
        
        rsf.expectedNodesList = Collections.singletonList(backingStrategy.resultElectSecondaries);
        replicaStorage = new MockReplicaStorage();
        replicaStorage.expectedKey = key;
        ReplicaInfo expectedReplicaInfo = new ReplicaInfo(node1, backingStrategy.resultElectSecondaries, payload);
        replicaStorage.expectedCreateReplicaInfo = expectedReplicaInfo;
        rsf.resultBuildStubWithNodesList = Collections.singletonList(replicaStorage);
        
        manager.create(key, payload);
        return expectedReplicaInfo;
    }
    
    private void assertReplicaInfo(ReplicaInfo expected, ReplicaInfo actual) {
        if (null == expected.getPrimary()) {
            assertNull(actual.getPrimary());
        } else {
            assertSame(expected.getPrimary(), actual.getPrimary());
        }
        
        NodeInfo[] expectedSecondaries = expected.getSecondaries();
        NodeInfo[] actualSecondaries = actual.getSecondaries();
        if (null == expectedSecondaries) {
            assertNull(expectedSecondaries);
        } else {
            assertEquals(expectedSecondaries.length, actualSecondaries.length);
            for (int i = 0; i < expectedSecondaries.length; i++) {
                assertEquals(expectedSecondaries[i], actualSecondaries[i]);
            }
        }
        
        assertSame(expected.getReplica(), actual.getReplica());
    }
    
    private class MockBackingStrategy implements BackingStrategy {
        private List secondaries = new ArrayList();
        
        private Object expectedKey;
        private NodeInfo[] resultElectSecondaries;
        private NodeInfo[] resultReElectSecondaries;
        private NodeInfo expectedReElectSecondariesPrimary;
        private NodeInfo[] expectedReElectSecondariesSecondaries;
        
        public NodeInfo[] electSecondaries(Object key) {
            assertSame(expectedKey, key);
            return resultElectSecondaries;
        }

        public NodeInfo[] reElectSecondaries(Object key, NodeInfo primary, NodeInfo[] secondaries) {
            assertSame(expectedKey, key);
            assertEquals(expectedReElectSecondariesPrimary, primary);
            assertEquals(expectedReElectSecondariesSecondaries.length, secondaries.length);
            for (int i = 0; i < expectedReElectSecondariesSecondaries.length; i++) {
                assertEquals(expectedReElectSecondariesSecondaries[i], secondaries[i]);
            }
            return resultReElectSecondaries;
        }

        public void addSecondaries(NodeInfo[] secondaries) {
        }

        public void addSecondary(NodeInfo secondary) {
            secondaries.add(secondary);
        }

        public void removeSecondary(NodeInfo secondary) {
            secondaries.remove(secondary);
        }
    }
    
    private class MockReplicationManagerFactory implements ReplicationManagerStubFactory {
        private ReplicationManager resultBuildStub;
        
        public ReplicationManager buildStub() {
            return resultBuildStub;
        }
    }

    private class MockReplicationManager implements ReplicationManager {
        private Object expectedKey;
        private ReplicaInfo resultReleasePrimary;

        public void create(Object key, Object tmp) {
            throw new UnsupportedOperationException();
        }

        public void update(Object key, Object tmp) {
            throw new UnsupportedOperationException();
        }

        public void destroy(Object key) {
            throw new UnsupportedOperationException();
        }

        public Object acquirePrimary(Object key) {
            throw new UnsupportedOperationException();
        }

        public ReplicaInfo releasePrimary(Object key) {
            assertSame(expectedKey, key);
            return resultReleasePrimary;
        }

        public ReplicaInfo retrieveReplicaInfo(Object key) {
            throw new UnsupportedOperationException();
        }
        
        public boolean managePrimary(Object key) {
            throw new UnsupportedOperationException();
        }

        public void start() throws Exception {
        }

        public void stop() throws Exception {
        }
    }
    
    
    private class MockReplicaStorageStubFactory implements ReplicaStorageStubFactory {
        private List expectedNodesList;
        private List resultBuildStubWithNodesList;
        private int index = 0;
        
        public ReplicaStorage buildStub() {
            throw new UnsupportedOperationException();
        }

        public ReplicaStorage buildStub(NodeInfo[] nodes) {
            NodeInfo[] expectedNodes = (NodeInfo[]) expectedNodesList.get(index);
            ReplicaStorage resultBuildStubWithNodes = (ReplicaStorage) resultBuildStubWithNodesList.get(index);
            index++;
            assertEquals(expectedNodes.length, nodes.length);
            for (int i = 0; i < expectedNodes.length; i++) {
                assertEquals(expectedNodes[i], nodes[i]);
            }
            return resultBuildStubWithNodes;
        }
    }
    
    private class MockReplicaStorage implements ReplicaStorage {
        private Object expectedKey;
        private ReplicaInfo expectedCreateReplicaInfo;
        private ReplicaInfo expectedUpdateReplicaInfo;

        public void mergeCreate(Object key, ReplicaInfo replicaInfo) {
            assertSame(expectedKey, key);
            assertReplicaInfo(expectedCreateReplicaInfo, replicaInfo);
        }

        public void mergeUpdate(Object key, ReplicaInfo replicaInfo) {
            assertSame(expectedKey, key);
            assertReplicaInfo(expectedUpdateReplicaInfo, replicaInfo);
        }

        public void mergeDestroy(Object key) {
            assertSame(expectedKey, key);
        }

        public ReplicaInfo retrieveReplicaInfo(Object key) {
            throw new UnsupportedOperationException();
        }

        public boolean storeReplicaInfo(Object key) {
            throw new UnsupportedOperationException();
        }

        public void purge() {
            throw new UnsupportedOperationException();
        }

        public NodeInfo getHostingNode() {
            throw new UnsupportedOperationException();
        }

        public void start() throws Exception {
        }

        public void stop() throws Exception {
        }
    }
    
    public class MockReplicaStorageMonitor implements ReplicaStorageMonitor {
        private ReplicaStorageListener listener;
        
        public void addReplicaStorageListener(ReplicaStorageListener listener) {
            this.listener = listener;
        }

        public void removeReplicaStorageListener(ReplicaStorageListener listener) {
            this.listener = null;
        }

        public void start() throws Exception {
        }

        public void stop() throws Exception {
        }
    }

    public class MockReplicationManagerExporter implements ReplicationManagerExporter {

        public void export(ReplicationManager manager) throws Exception {
        }

        public void unexport(ReplicationManager manager) throws Exception {
        }
    }
}
