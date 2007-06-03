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

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMPeer;

import junit.framework.TestCase;

public class ReplicaInfoTest extends TestCase {
    private static final Peer peer1 = new VMPeer("PEER1", null);
    private static final Peer peer2 = new VMPeer("PEER2", null);
    private static final Peer peer3 = new VMPeer("PEER3", null);
    
    public void testCreateWithPrototpye() {
        Object replica = new Object();
        ReplicaInfo info = new ReplicaInfo(peer1, new Peer[] {peer2}, replica);
        info = new ReplicaInfo(info, new Peer[] {peer3});
        assertSame(peer1, info.getPrimary());
        assertEquals(1, info.getSecondaries().length);
        assertSame(peer3, info.getSecondaries()[0]);
        assertSame(replica, info.getReplica());
        assertEquals(1, info.getVersion());
    }

    public void testCreateWithReplica() {
        ReplicaInfo info = new ReplicaInfo(peer1, new Peer[] {peer2}, new Object());
        Object replica = new Object();
        info = new ReplicaInfo(info, replica);
        assertSame(peer1, info.getPrimary());
        assertEquals(1, info.getSecondaries().length);
        assertSame(peer2, info.getSecondaries()[0]);
        assertSame(replica, info.getReplica());
        assertEquals(1, info.getVersion());
    }

    public void testCreateWithPrimaryAndSecondaries() {
        Object replica = new Object();
        ReplicaInfo info = new ReplicaInfo(peer1, new Peer[] {peer2}, replica);
        info = new ReplicaInfo(info, peer2, new Peer[] {peer3});
        assertSame(peer2, info.getPrimary());
        assertEquals(1, info.getSecondaries().length);
        assertSame(peer3, info.getSecondaries()[0]);
        assertSame(replica, info.getReplica());
        assertEquals(1, info.getVersion());
    }

}
