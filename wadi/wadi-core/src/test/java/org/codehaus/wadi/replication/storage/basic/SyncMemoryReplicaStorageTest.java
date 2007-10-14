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

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMPeer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.common.ReplicaStorageInfo;
import org.codehaus.wadi.replication.manager.basic.ObjectStateHandler;
import org.codehaus.wadi.replication.storage.memory.SyncMemoryReplicaStorage;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision$
 */
public class SyncMemoryReplicaStorageTest extends RMockTestCase {
    private SyncMemoryReplicaStorage storage;
    private ObjectStateHandler objectStateManager;
    private Peer node1;
    private Peer node2;
    private Object payload;
    private Object key;
    private ReplicaInfo replicaInfo;
    private byte[] serializedState;

    protected void setUp() throws Exception {
        objectStateManager = (ObjectStateHandler) mock(ObjectStateHandler.class);
        storage = new SyncMemoryReplicaStorage(objectStateManager);
        node1 = new VMPeer("node1", null);
        node2 = new VMPeer("node2", null);
        
        payload = new Object();
        key = new Object();
        replicaInfo = new ReplicaInfo(node1, new Peer[] {node2}, new Object());
        serializedState = new byte[0];
    }
  
    public void testInsert() throws Exception {
        objectStateManager.initState(key, replicaInfo.getPayload());
        startVerification();
        
        storage.insert(key, replicaInfo);
    }
    
    public void testMergeCreate() {
        objectStateManager.restoreFromFullState(key, serializedState);
        modify().returnValue(payload);
        
        objectStateManager.extractFullState(key, payload);
        modify().returnValue(serializedState);
        
        startVerification();
        
        storage.mergeCreate(key, new ReplicaStorageInfo(replicaInfo, serializedState));
        ReplicaStorageInfo storageInfo = storage.retrieveReplicaStorageInfo(key);
        assertNotNull(storageInfo);
        assertSame(serializedState, storageInfo.getSerializedPayload());
        assertReplicaInfo(new ReplicaInfo(node1, new Peer[] {node2}, payload), storageInfo.getReplicaInfo());
    }

    public void testMergeUpdate() {
        objectStateManager.restoreFromFullState(key, serializedState);
        modify().returnValue(new Object());
        
        byte[] updateSerializedState = new byte[1];
        objectStateManager.restoreFromUpdatedState(key, updateSerializedState);
        modify().returnValue(payload);
        
        objectStateManager.extractFullState(key, payload);
        modify().returnValue(serializedState);

        startVerification();
        
        storage.mergeCreate(key, new ReplicaStorageInfo(replicaInfo, serializedState));
        storage.mergeUpdate(key, new ReplicaStorageInfo(replicaInfo, updateSerializedState));
        
        ReplicaStorageInfo storageInfo = storage.retrieveReplicaStorageInfo(key);
        assertNotNull(storageInfo);
        assertReplicaInfo(new ReplicaInfo(node1, new Peer[] {node2}, payload), storageInfo.getReplicaInfo());
    }

    public void testMergeDestroy() {
        objectStateManager.restoreFromFullState(key, serializedState);
        modify().returnValue(payload);

        objectStateManager.discardState(key, payload);
        
        startVerification();
        
        storage.mergeCreate(key, new ReplicaStorageInfo(replicaInfo, serializedState));
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
        
        assertSame(expected.getPayload(), actual.getPayload());
    }
    
}
