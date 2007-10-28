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
package org.codehaus.wadi.replication.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import junit.framework.TestCase;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.PeerInfo;

public class ReplicaStorageInfoTest extends TestCase {
    private static final Peer peer1 = new MockPeer("PEER1");
    private static final Peer peer2 = new MockPeer("PEER2");
    
    public void testExternalizable() throws Exception {
        ReplicaInfo info = new ReplicaInfo(peer1, new Peer[] {peer2}, new Object());
        info.increaseVersion();
        
        ReplicaStorageInfo storageInfo = new ReplicaStorageInfo(info, new byte[] {1, 2});
        
        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(memOut);
        out.writeObject(storageInfo);
        out.close();
        
        ByteArrayInputStream memIn = new ByteArrayInputStream(memOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(memIn);
        ReplicaStorageInfo newStorageInfo = (ReplicaStorageInfo) in.readObject();
        ReplicaInfo newInfo = newStorageInfo.getReplicaInfo();
        assertEquals(peer1.getName(), newInfo.getPrimary().getName());
        assertEquals(1, newInfo.getSecondaries().length);
        assertEquals(peer2.getName(), newInfo.getSecondaries()[0].getName());
        assertEquals(info.getVersion(), newInfo.getVersion());
        
        assertEquals(2, newStorageInfo.getSerializedPayload().length);
        assertEquals(1, newStorageInfo.getSerializedPayload()[0]);
        assertEquals(2, newStorageInfo.getSerializedPayload()[1]);
    }
    
    private static class MockPeer implements Peer, Serializable {
        private final String name;
        
        public MockPeer(String name) {
            this.name = name;
        }

        public Address getAddress() {
            return null;
        }

        public String getName() {
            return name;
        }

        public PeerInfo getPeerInfo() {
            return null;
        }
        
    }
    
}
