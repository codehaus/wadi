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
package org.codehaus.wadi.partition;

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMPeer;

import junit.framework.TestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class PartitionBalancingInfoTest extends TestCase {
    private static final VMPeer PEER1 = new VMPeer("peer1", null);
    private static final VMPeer PEER2 = new VMPeer("peer2", null);
    
    private PartitionBalancingInfo balancingInfo;
    
    protected void setUp() throws Exception {
        PartitionInfo[] partitionInfos = newPartitionInfo(1, new Peer[] {PEER1, PEER2, PEER2});
        balancingInfo = new PartitionBalancingInfo(partitionInfos);
        balancingInfo = new PartitionBalancingInfo(PEER1, balancingInfo);
    }
    
    public void testGetNumberOfLocalPartitionInfos() {
        int actualLocal = balancingInfo.getNumberOfLocalPartitionInfos();
        assertEquals(1, actualLocal);
    }   

    public void testGetLocalPartitionInfos() {
        PartitionInfo[] localPartitionInfos = balancingInfo.getLocalPartitionInfos();
        assertEquals(1, localPartitionInfos.length);
        assertEquals(0, localPartitionInfos[0].getIndex());
    }
    
    public void testGetNumberOfPartitionsOwnedBy() {
        int actualOwnedBy = balancingInfo.getNumberOfPartitionsOwnedBy(PEER2);
        assertEquals(2, actualOwnedBy);
    }

    public void testGetPartitionsOwnedBy() {
        PartitionInfo[] ownedByPartitionInfos = balancingInfo.getPartitionsOwnedBy(PEER2);
        assertEquals(2, ownedByPartitionInfos.length);
        PartitionInfo partitionInfo = ownedByPartitionInfos[0];
        assertSame(PEER2, partitionInfo.getOwner());
        assertEquals(1, partitionInfo.getIndex());
        partitionInfo = ownedByPartitionInfos[1];
        assertSame(PEER2, partitionInfo.getOwner());
        assertEquals(2, partitionInfo.getIndex());
    }
    
    private PartitionInfo[] newPartitionInfo(int version, Peer[] peers) {
        PartitionInfo[] partitionInfos = new PartitionInfo[peers.length];
        for (int i = 0; i < peers.length; i++) {
            partitionInfos[i] = new PartitionInfo(version, i, peers[i]);
        }
        return partitionInfos;
    }
}
