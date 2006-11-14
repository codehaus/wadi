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
package org.codehaus.wadi.replication.storage.basic;

import junit.framework.TestCase;

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMPeer;
import org.codehaus.wadi.replication.common.ReplicaInfo;

public class MemoryReplicaStorageTest extends TestCase {
    private MemoryReplicaStorage storage;
    private Peer node1;
    private Peer node2;

    protected void setUp() throws Exception {
        storage = new MemoryReplicaStorage();
        node1 = new VMPeer("node1", null);
        node2 = new VMPeer("node2", null);
    }
    
    public void testMergeCreate() {
        Object key = new Object();
        Object replica = new Object();
        ReplicaInfo replicaInfo = new ReplicaInfo(node1, new Peer[] {node2}, replica);
        storage.mergeCreate(key, replicaInfo);
        
        assertTrue(storage.storeReplicaInfo(key));
        ReplicaInfo actualReplicaInfo = storage.retrieveReplicaInfo(key);
        assertNotNull(actualReplicaInfo);
        assertReplicaInfo(replicaInfo, actualReplicaInfo);
    }

    public void testMergeUpdate() {
        Object key = new Object();
        Object replica = new Object();
        ReplicaInfo replicaInfo = new ReplicaInfo(node1, new Peer[] {node2}, replica);
        storage.mergeCreate(key, replicaInfo);

        replica = new Object();
        replicaInfo = new ReplicaInfo(node1, new Peer[] {node2}, replica);
        storage.mergeUpdate(key, new ReplicaInfo(node1, new Peer[] {node2}, replica));
        
        assertTrue(storage.storeReplicaInfo(key));
        ReplicaInfo actualReplicaInfo = storage.retrieveReplicaInfo(key);
        assertNotNull(actualReplicaInfo);
        assertReplicaInfo(new ReplicaInfo(node1, new Peer[] {node2}, replica), actualReplicaInfo);
    }

    public void testMergeDestroy() {
        Object key = new Object();
        Object replica = new Object();
        ReplicaInfo replicaInfo = new ReplicaInfo(node1, new Peer[] {node2}, replica);
        storage.mergeCreate(key, replicaInfo);

        storage.mergeDestroy(key);
        assertFalse(storage.storeReplicaInfo(key));
    }

    private void assertReplicaInfo(ReplicaInfo expected, ReplicaInfo actual) {
        if (null == expected.getPrimary()) {
            assertNull(actual.getPrimary());
        } else {
            assertSame(expected.getPrimary(), actual.getPrimary());
        }
        
        Peer[] expectedSecondaries = expected.getSecondaries();
        Peer[] actualSecondaries = actual.getSecondaries();
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
    
}
