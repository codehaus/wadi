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

import junit.framework.TestCase;

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMPeer;
import org.codehaus.wadi.location.balancing.PartitionBalancingInfo;
import org.codehaus.wadi.location.balancing.PartitionInfo;
import org.codehaus.wadi.location.balancing.PartitionInfoUpdate;
import org.codehaus.wadi.location.balancing.PartitionInfoUpdateBuilder;
import org.codehaus.wadi.location.balancing.PartitionInfoUpdates;

/**
 * 
 * @version $Revision: 1538 $
 */
public class PartitionBalancingInfoBuilderTest extends TestCase {
    private static final VMPeer PEER1 = new VMPeer("peer1", null);
    private static final VMPeer PEER2 = new VMPeer("peer2", null);
    
    public void testMergePartitionInfos() {
        PartitionInfoUpdateBuilder builder = new PartitionInfoUpdateBuilder(3, 2);
        PartitionInfo[] partitionInfos = newPartitionInfo(1, new Peer[] {PEER1, PEER2, PEER2});
        PartitionBalancingInfo toMerge = new PartitionBalancingInfo(partitionInfos);
        toMerge = new PartitionBalancingInfo(PEER1, toMerge);
        builder.mergePartitionInfos(toMerge);
        
        partitionInfos = newPartitionInfo(1, new Peer[] {PEER1, PEER2, PEER2});
        toMerge = new PartitionBalancingInfo(partitionInfos);
        toMerge = new PartitionBalancingInfo(PEER2, toMerge);
        builder.mergePartitionInfos(toMerge);

        PartitionBalancingInfo resultingPartitionInfo = build(builder);

        assertResultingPartitionInfo(resultingPartitionInfo, new Peer[] {PEER1, PEER2, PEER2});
    }

    public void testConflictMerge() {
        PartitionInfoUpdateBuilder builder = new PartitionInfoUpdateBuilder(3, 2);
        PartitionInfo[] partitionInfos = newPartitionInfo(1, new Peer[] {PEER1, PEER1, PEER2});
        PartitionBalancingInfo toMerge = new PartitionBalancingInfo(partitionInfos);
        toMerge = new PartitionBalancingInfo(PEER1, toMerge);
        builder.mergePartitionInfos(toMerge);
        
        partitionInfos = newPartitionInfo(1, new Peer[] {PEER1, PEER2, PEER2});
        toMerge = new PartitionBalancingInfo(partitionInfos);
        toMerge = new PartitionBalancingInfo(PEER2, toMerge);
        
        try {
            builder.mergePartitionInfos(toMerge);
            fail();
        } catch (IllegalStateException e) {
        }
    }
    
    public void testAddPartitionInfos() {
        PartitionInfoUpdateBuilder builder = new PartitionInfoUpdateBuilder(3, 2);
        builder.addPartitionInfos(PEER1, 1);
        builder.addPartitionInfos(PEER2, 2);
        
        PartitionBalancingInfo resultingPartitionInfo = build(builder);
        
        assertResultingPartitionInfo(resultingPartitionInfo, new Peer[] {PEER1, PEER2, PEER2});
    }

    public void testCannotAddTooManyPartition() {
        PartitionInfoUpdateBuilder builder = new PartitionInfoUpdateBuilder(3, 2);
        builder.addPartitionInfos(PEER1, 2);
        builder.addPartitionInfos(PEER2, 2);
        
        try {
            builder.build();
            fail();
        } catch (IllegalStateException e) {
        }
    }

    public void testRemovePartitions() {
        PartitionInfoUpdateBuilder builder = new PartitionInfoUpdateBuilder(3, 2);
        PartitionInfo[] partitionInfos = newPartitionInfo(1, new Peer[] {PEER1, PEER1, PEER1});
        PartitionBalancingInfo toRemove = new PartitionBalancingInfo(partitionInfos);
        toRemove = new PartitionBalancingInfo(PEER1, toRemove);
        builder.removePartitions(toRemove, 1);

        builder.addPartitionInfos(PEER2, 1);
        
        PartitionBalancingInfo resultingPartitionInfo = build(builder);
        
        assertResultingPartitionInfo(resultingPartitionInfo, new Peer[] {PEER2, PEER1, PEER1});
    }
    
    public void testCannotRemoveTooManyPartitions() {
        PartitionInfoUpdateBuilder builder = new PartitionInfoUpdateBuilder(3, 2);
        PartitionInfo[] partitionInfos = newPartitionInfo(1, new Peer[] {PEER1, PEER2, PEER2});
        PartitionBalancingInfo toRemove = new PartitionBalancingInfo(partitionInfos);
        toRemove = new PartitionBalancingInfo(PEER1, toRemove);
        try {
            builder.removePartitions(toRemove, 2);
            fail();
        } catch (IllegalStateException e) {
        }
    }
    
    private void assertResultingPartitionInfo(PartitionBalancingInfo resultingPartitionInfo, Peer[] peers) {
        PartitionInfo[] partitionInfos = resultingPartitionInfo.getPartitionInfos();
        for (int i = 0; i < partitionInfos.length; i++) {
            PartitionInfo partitionInfo = partitionInfos[i];
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
    
    private PartitionBalancingInfo build(PartitionInfoUpdateBuilder builder) {
        PartitionInfoUpdates infoUpdates = builder.build();
        PartitionInfoUpdate[] updates = infoUpdates.getPartitionUpdates();
        PartitionInfo[] partitionInfos = new PartitionInfo[updates.length];
        for (int i = 0; i < updates.length; i++) {
            partitionInfos[i] = updates[i].getPartitionInfo();
        }
        return new PartitionBalancingInfo(partitionInfos);
    }
}
