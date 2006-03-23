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

import junit.framework.TestCase;

public class ReplicaInfoTest extends TestCase {
    private static final NodeInfo node1 = new NodeInfo("NODE1");
    private static final NodeInfo node2 = new NodeInfo("NODE2");
    private static final NodeInfo node3 = new NodeInfo("NODE3");
    
    public void testCreateWithReplica() {
        ReplicaInfo info = new ReplicaInfo(node1, new NodeInfo[] {node2}, null);
        Object replica = new Object();
        info = new ReplicaInfo(info, replica);
        assertSame(node1, info.getPrimary());
        assertEquals(1, info.getSecondaries().length);
        assertSame(node2, info.getSecondaries()[0]);
        assertSame(replica, info.getReplica());
    }

    public void testCreateWithPrimaryAndSecondaries() {
        Object replica = new Object();
        ReplicaInfo info = new ReplicaInfo(node1, new NodeInfo[] {node2}, replica);
        info = new ReplicaInfo(info, node2, new NodeInfo[] {node3});
        assertSame(node2, info.getPrimary());
        assertEquals(1, info.getSecondaries().length);
        assertSame(node3, info.getSecondaries()[0]);
        assertSame(replica, info.getReplica());
    }

    public void testCreateWithOverride() {
        ReplicaInfo info = new ReplicaInfo(node1, new NodeInfo[] {node2}, null);
        Object replica = new Object();
        info = new ReplicaInfo(info, new ReplicaInfo(node2, new NodeInfo[] {node3}, replica));
        assertSame(node2, info.getPrimary());
        assertEquals(1, info.getSecondaries().length);
        assertSame(node3, info.getSecondaries()[0]);
        assertSame(replica, info.getReplica());
    }
}
