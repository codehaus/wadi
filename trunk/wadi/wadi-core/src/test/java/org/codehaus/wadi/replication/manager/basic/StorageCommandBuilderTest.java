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

import junit.framework.TestCase;

import org.codehaus.wadi.replication.common.NodeInfo;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.storage.ReplicaStorageStubFactory;

public class StorageCommandBuilderTest extends TestCase {

    public void testBuild() throws Exception {
        final NodeInfo node1 = new NodeInfo("node1");
        final NodeInfo node2 = new NodeInfo("node2");
        final NodeInfo node3 = new NodeInfo("node3");
        final NodeInfo node4 = new NodeInfo("node4");
        final String expectedKey = "key";
        Object payload = new Object();
        final ReplicaInfo expectedReplicaInfo = new ReplicaInfo(node1, new NodeInfo[] {node2, node3}, payload);
        StorageCommandBuilder builder = new StorageCommandBuilder(expectedKey,
                expectedReplicaInfo,
                new NodeInfo[] {node3, node4});
        
        StorageCommand[] commands = builder.build();
        assertEquals(3, commands.length);

        StorageCommand command = commands[0];
        MockReplicaStorageStubFactory stubFactory = new MockReplicaStorageStubFactory();
        stubFactory.expectedNodes = new NodeInfo[] {node2};
        stubFactory.storage = new MockReplicaStorage() {
            public void mergeCreate(Object key, ReplicaInfo replicaInfo) {
                assertSame(expectedKey, key);
                assertSame(expectedReplicaInfo, replicaInfo);
            };
        };
        command.execute(stubFactory);
        
        command = commands[1];
        stubFactory.expectedNodes = new NodeInfo[] {node3};
        stubFactory.storage = new MockReplicaStorage() {
            public void mergeUpdate(Object key, ReplicaInfo replicaInfo) {
                assertSame(expectedKey, key);
                assertSame(expectedReplicaInfo.getPrimary(), replicaInfo.getPrimary());
                assertSame(expectedReplicaInfo.getSecondaries(), replicaInfo.getSecondaries());
                assertNull(replicaInfo.getReplica());
            };
        };
        command.execute(stubFactory);

        command = commands[2];
        stubFactory.expectedNodes = new NodeInfo[] {node4};
        stubFactory.storage = new MockReplicaStorage() {
            public void mergeDestroy(Object key) {
                assertSame(expectedKey, key);
            };
        };
        command.execute(stubFactory);
    }
    
    private class MockReplicaStorageStubFactory implements ReplicaStorageStubFactory {
        private NodeInfo[] expectedNodes;
        private ReplicaStorage storage;
        
        public void start() {
        }
        
        public void stop() {
        }
        
        public ReplicaStorage buildStub() {
            throw new AssertionError();
        }

        public ReplicaStorage buildStub(NodeInfo[] nodes) {
            assertEquals(expectedNodes.length, nodes.length);
            for (int i = 0; i < nodes.length; i++) {
                assertEquals(expectedNodes[i], nodes[i]);
            }
            return storage;
        }
    }
    
    private class MockReplicaStorage implements ReplicaStorage {

        public void mergeCreate(Object key, ReplicaInfo replicaInfo) {
            throw new AssertionError();
        }

        public void mergeUpdate(Object key, ReplicaInfo replicaInfo) {
            throw new AssertionError();
        }

        public void mergeDestroy(Object key) {
            throw new AssertionError();
        }

        public ReplicaInfo retrieveReplicaInfo(Object key) {
            throw new AssertionError();
        }

        public boolean storeReplicaInfo(Object key) {
            throw new AssertionError();
        }

        public NodeInfo getHostingNode() {
            throw new AssertionError();
        }

        public void start() throws Exception {
            throw new AssertionError();
        }

        public void stop() throws Exception {
            throw new AssertionError();
        }
    }
}
