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
import java.util.Map;

import junit.framework.TestCase;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.PeerInfo;

public class ReplicaInfoTest extends TestCase {
    private static final Peer peer1 = new MockPeer("PEER1");
    private static final Peer peer2 = new MockPeer("PEER2");
    private static final Peer peer3 = new MockPeer("PEER3");
    
    public void testUpdateSecondaries() throws Exception {
        Object replica = new Object();
        Peer[] secondaries = new Peer[] {peer2};
        ReplicaInfo info = new ReplicaInfo(peer1, secondaries, replica);
        assertEquals(0, info.getVersion());
        
        secondaries = new Peer[] {peer3};
        info.updateSecondaries(secondaries);
        assertEquals(1, info.getVersion());
    }
    
    public void testCreateWithPrototype() {
        Object payload = new Object();
        Peer primary = peer1;
        Peer[] secondaries = new Peer[] {peer2};
        ReplicaInfo info = new ReplicaInfo(primary, secondaries, payload);
        assertEquals(0, info.getVersion());
        
        primary = peer2;
        secondaries = new Peer[] {peer3};
        info = new ReplicaInfo(info, primary, secondaries);
        assertSame(primary, info.getPrimary());
        assertSame(secondaries, info.getSecondaries());
        assertSame(payload, info.getPayload());
        assertEquals(1, info.getVersion());
    }

    public void testExternalizable() throws Exception {
        ReplicaInfo info = new ReplicaInfo(peer1, new Peer[] {peer2}, new Object());
        info.increaseVersion();
        
        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(memOut);
        out.writeObject(info);
        out.close();
        
        ByteArrayInputStream memIn = new ByteArrayInputStream(memOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(memIn);
        ReplicaInfo newInfo = (ReplicaInfo) in.readObject();
        assertEquals(peer1.getName(), newInfo.getPrimary().getName());
        assertEquals(1, newInfo.getSecondaries().length);
        assertEquals(peer2.getName(), newInfo.getSecondaries()[0].getName());
        assertEquals(info.getVersion(), newInfo.getVersion());
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

        public Map<Object, Object> getLocalStateMap() {
            return null;
        }
        
    }
    
}
