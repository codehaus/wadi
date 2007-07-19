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

}
