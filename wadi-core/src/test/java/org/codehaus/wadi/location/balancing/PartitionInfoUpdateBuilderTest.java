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
package org.codehaus.wadi.location.balancing;

import java.util.BitSet;

import junit.framework.TestCase;

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMPeer;

/**
 * 
 * @version $Revision: 1538 $
 */
public class PartitionInfoUpdateBuilderTest extends TestCase {
    private static final VMPeer PEER1 = new VMPeer("peer1", null);
    private static final VMPeer PEER2 = new VMPeer("peer2", null);
    private PartitionInfoUpdateBuilder builder;
    
    @Override
    protected void setUp() throws Exception {
        builder = new PartitionInfoUpdateBuilder(3, 2, new BitSet(3));
    }
    
    public void testMergePartitionInfos() {
        PartitionInfo[] partitionInfos = newPartitionInfo(1, new Peer[] {PEER1, PEER2, PEER2});
        PartitionBalancingInfo toMerge = new PartitionBalancingInfo(partitionInfos);
        toMerge = new PartitionBalancingInfo(PEER1, toMerge);
        builder.mergePartitionInfos(toMerge);
        
        partitionInfos = newPartitionInfo(1, new Peer[] {PEER1, PEER2, PEER2});
        toMerge = new PartitionBalancingInfo(partitionInfos);
        toMerge = new PartitionBalancingInfo(PEER2, toMerge);
        builder.mergePartitionInfos(toMerge);

        PartitionInfoUpdate[] updates = build(builder);

        assertUpdates(updates, new Peer[] {PEER1, PEER2, PEER2});
    }

    public void testAddPartitionInfos() {
        builder.addPartitionInfos(PEER1, 1);
        builder.addPartitionInfos(PEER2, 2);
        
        PartitionInfoUpdate[] updates = build(builder);
        
        assertUpdates(updates, new Peer[] {PEER1, PEER2, PEER2});
    }

    public void testCannotAddTooManyPartition() {
        builder.addPartitionInfos(PEER1, 2);
        builder.addPartitionInfos(PEER2, 2);
        
        try {
            builder.build();
            fail();
        } catch (IllegalStateException e) {
        }
    }

    public void testRemovePartitions() {
        PartitionInfo[] partitionInfos = newPartitionInfo(1, new Peer[] {PEER1, PEER1, PEER1});
        PartitionBalancingInfo toRemove = new PartitionBalancingInfo(partitionInfos);
        toRemove = new PartitionBalancingInfo(PEER1, toRemove);
        builder.removePartitions(toRemove, 1);

        builder.addPartitionInfos(PEER2, 1);
        
        PartitionInfoUpdate[] updates = build(builder);
        
        assertUpdates(updates, new Peer[] {PEER2, PEER1, PEER1});
    }
    
    public void testCannotRemoveTooManyPartitions() {
        PartitionInfo[] partitionInfos = newPartitionInfo(1, new Peer[] {PEER1, PEER2, PEER2});
        PartitionBalancingInfo toRemove = new PartitionBalancingInfo(partitionInfos);
        toRemove = new PartitionBalancingInfo(PEER1, toRemove);
        try {
            builder.removePartitions(toRemove, 2);
            fail();
        } catch (IllegalStateException e) {
        }
    }
    
    public void testMergePartitions() {
        PartitionInfo[] partitionInfos = newPartitionInfo(1, new Peer[] {PEER1, PEER1, PEER2});
        PartitionBalancingInfo toMerge = new PartitionBalancingInfo(partitionInfos);
        toMerge = new PartitionBalancingInfo(PEER1, toMerge);
        builder.mergePartitionInfos(toMerge);
        
        partitionInfos = newPartitionInfo(1, new Peer[] {PEER1, PEER2, PEER2});
        toMerge = new PartitionBalancingInfo(partitionInfos);
        toMerge = new PartitionBalancingInfo(PEER2, toMerge);
        builder.mergePartitionInfos(toMerge);

        PartitionInfoUpdate[] updates = build(builder);

        assertUpdates(updates, new Peer[] {PEER1, PEER1, PEER2});
        assertEquals(1, updates[1].getPartitionInfo().getNumberOfExpectedMerge());
    }
    
    private void assertUpdates(PartitionInfoUpdate[] updates, Peer[] peers) {
        for (int i = 0; i < updates.length; i++) {
            PartitionInfo partitionInfo = updates[i].getPartitionInfo();
            assertSame(peers[i], partitionInfo.getOwner());
        }
    }
    
    private PartitionInfo[] newPartitionInfo(int version, Peer[] peers) {
        PartitionInfo[] partitionInfos = new PartitionInfo[peers.length];
        for (int i = 0; i < peers.length; i++) {
            partitionInfos[i] = new PartitionInfo(version, i, peers[i]);
        }
        return partitionInfos;
    }
    
    private PartitionInfoUpdate[] build(PartitionInfoUpdateBuilder builder) {
        PartitionInfoUpdates infoUpdates = builder.build();
        return infoUpdates.getPartitionUpdates();
    }
}
