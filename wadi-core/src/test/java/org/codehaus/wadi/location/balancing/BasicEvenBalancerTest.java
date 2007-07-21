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

import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMPeer;
import org.codehaus.wadi.location.balancing.BasicEvenBalancer;
import org.codehaus.wadi.location.balancing.PartitionBalancingInfo;
import org.codehaus.wadi.location.balancing.PartitionBalancingInfoState;
import org.codehaus.wadi.location.balancing.PartitionInfo;
import org.codehaus.wadi.location.balancing.PartitionInfoUpdate;
import org.codehaus.wadi.location.balancing.PartitionInfoUpdates;

import junit.framework.TestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicEvenBalancerTest extends TestCase {
    private static final Peer PEER1 = new VMPeer("peer1", null);
    private static final Peer PEER2 = new VMPeer("peer2", null);
    private static final Peer PEER3 = new VMPeer("peer3", null);
    private static final Peer PEER4 = new VMPeer("peer4", null);
    private static final int NB_PARTITIONS = 12;
    
    public void testBalancingAddPeer() throws Exception {
        Map peerToBalancingInfo = new HashMap();
        peerToBalancingInfo.put(PEER1, newBasicState(new UnknownPartitionBalancingInfo(PEER1, NB_PARTITIONS)));
        BasicEvenBalancer balancer = new BasicEvenBalancer(NB_PARTITIONS, peerToBalancingInfo);
        PartitionBalancingInfo resultingBalancingInfo = balance(balancer);

        assertEquals(12, resultingBalancingInfo.getPartitionsOwnedBy(PEER1).length);
        assertVersion(1, resultingBalancingInfo);
        
        peerToBalancingInfo = new HashMap();
        peerToBalancingInfo.put(PEER1, newBasicState(new PartitionBalancingInfo(PEER1, resultingBalancingInfo)));
        peerToBalancingInfo.put(PEER2, newBasicState(new UnknownPartitionBalancingInfo(PEER2, NB_PARTITIONS)));
        balancer = new BasicEvenBalancer(NB_PARTITIONS, peerToBalancingInfo);
        resultingBalancingInfo = balance(balancer);
        assertVersion(2, resultingBalancingInfo);
        
        assertEquals(6, resultingBalancingInfo.getPartitionsOwnedBy(PEER1).length);
        assertEquals(6, resultingBalancingInfo.getPartitionsOwnedBy(PEER2).length);
        
        peerToBalancingInfo = new HashMap();
        peerToBalancingInfo.put(PEER1, newBasicState(new PartitionBalancingInfo(PEER1, resultingBalancingInfo)));
        peerToBalancingInfo.put(PEER2, newBasicState(new PartitionBalancingInfo(PEER2, resultingBalancingInfo)));
        peerToBalancingInfo.put(PEER3, newBasicState(new UnknownPartitionBalancingInfo(PEER3, NB_PARTITIONS)));
        balancer = new BasicEvenBalancer(NB_PARTITIONS, peerToBalancingInfo);
        resultingBalancingInfo = balance(balancer);
        assertVersion(3, resultingBalancingInfo);
        
        assertEquals(4, resultingBalancingInfo.getPartitionsOwnedBy(PEER1).length);
        assertEquals(4, resultingBalancingInfo.getPartitionsOwnedBy(PEER2).length);
        assertEquals(4, resultingBalancingInfo.getPartitionsOwnedBy(PEER3).length);

        peerToBalancingInfo = new HashMap();
        peerToBalancingInfo.put(PEER1, newBasicState(new PartitionBalancingInfo(PEER1, resultingBalancingInfo)));
        peerToBalancingInfo.put(PEER2, newBasicState(new PartitionBalancingInfo(PEER2, resultingBalancingInfo)));
        peerToBalancingInfo.put(PEER3, newBasicState(new PartitionBalancingInfo(PEER3, resultingBalancingInfo)));
        peerToBalancingInfo.put(PEER4, newBasicState(new UnknownPartitionBalancingInfo(PEER4, NB_PARTITIONS)));
        balancer = new BasicEvenBalancer(NB_PARTITIONS, peerToBalancingInfo);
        resultingBalancingInfo = balance(balancer);
        assertVersion(4, resultingBalancingInfo);

        assertEquals(3, resultingBalancingInfo.getPartitionsOwnedBy(PEER1).length);
        assertEquals(3, resultingBalancingInfo.getPartitionsOwnedBy(PEER2).length);
        assertEquals(3, resultingBalancingInfo.getPartitionsOwnedBy(PEER3).length);
        assertEquals(3, resultingBalancingInfo.getPartitionsOwnedBy(PEER4).length);
    }

    public void testBalancingRemovePeer() throws Exception {
        Map peerToBalancingInfo = new HashMap();
        peerToBalancingInfo.put(PEER1, newBasicState(new UnknownPartitionBalancingInfo(PEER1, NB_PARTITIONS)));
        peerToBalancingInfo.put(PEER2, newBasicState(new UnknownPartitionBalancingInfo(PEER2, NB_PARTITIONS)));
        peerToBalancingInfo.put(PEER3, newBasicState(new UnknownPartitionBalancingInfo(PEER3, NB_PARTITIONS)));
        peerToBalancingInfo.put(PEER4, newBasicState(new UnknownPartitionBalancingInfo(PEER4, NB_PARTITIONS)));
        BasicEvenBalancer balancer = new BasicEvenBalancer(NB_PARTITIONS, peerToBalancingInfo);
        PartitionBalancingInfo resultingBalancingInfo = balance(balancer);
        assertVersion(1, resultingBalancingInfo);
        
        peerToBalancingInfo = new HashMap();
        peerToBalancingInfo.put(PEER1, newBasicState(new PartitionBalancingInfo(PEER1, resultingBalancingInfo)));
        peerToBalancingInfo.put(PEER2, newBasicState(new PartitionBalancingInfo(PEER2, resultingBalancingInfo)));
        peerToBalancingInfo.put(PEER3, newBasicState(new PartitionBalancingInfo(PEER3, resultingBalancingInfo)));
        balancer = new BasicEvenBalancer(NB_PARTITIONS, peerToBalancingInfo);
        resultingBalancingInfo = balance(balancer);
        assertVersion(2, resultingBalancingInfo);
        
        assertEquals(4, resultingBalancingInfo.getPartitionsOwnedBy(PEER1).length);
        assertEquals(4, resultingBalancingInfo.getPartitionsOwnedBy(PEER2).length);
        assertEquals(4, resultingBalancingInfo.getPartitionsOwnedBy(PEER3).length);
        
        peerToBalancingInfo = new HashMap();
        peerToBalancingInfo.put(PEER1, newBasicState(new PartitionBalancingInfo(PEER1, resultingBalancingInfo)));
        peerToBalancingInfo.put(PEER2, newBasicState(new PartitionBalancingInfo(PEER2, resultingBalancingInfo)));
        balancer = new BasicEvenBalancer(NB_PARTITIONS, peerToBalancingInfo);
        resultingBalancingInfo = balance(balancer);
        assertVersion(3, resultingBalancingInfo);
        
        assertEquals(6, resultingBalancingInfo.getPartitionsOwnedBy(PEER1).length);
        assertEquals(6, resultingBalancingInfo.getPartitionsOwnedBy(PEER2).length);

        peerToBalancingInfo = new HashMap();
        peerToBalancingInfo.put(PEER1, newBasicState(new PartitionBalancingInfo(PEER1, resultingBalancingInfo)));
        balancer = new BasicEvenBalancer(NB_PARTITIONS, peerToBalancingInfo);
        resultingBalancingInfo = balance(balancer);
        assertVersion(4, resultingBalancingInfo);

        assertEquals(12, resultingBalancingInfo.getPartitionsOwnedBy(PEER1).length);
    }
    
    public void testEvacuatingPeersAreSkipped() throws Exception {
        Map peerToBalancingInfo = new HashMap();
        peerToBalancingInfo.put(PEER1, newBasicState(new UnknownPartitionBalancingInfo(PEER1, NB_PARTITIONS)));
        peerToBalancingInfo.put(PEER2, new PartitionBalancingInfoState(true, new UnknownPartitionBalancingInfo(PEER1, NB_PARTITIONS)));
        BasicEvenBalancer balancer = new BasicEvenBalancer(NB_PARTITIONS, peerToBalancingInfo);
        PartitionBalancingInfo resultingBalancingInfo = balance(balancer);
        
        assertEquals(12, resultingBalancingInfo.getPartitionsOwnedBy(PEER1).length);
    }

    public void testRepopulatePartitions() throws Exception {
        int version = 3;
        
        Map peerToBalancingInfo = new HashMap();
        PartitionInfo[] partitionInfos = new PartitionInfo[NB_PARTITIONS];
        for (int i = 0; i < 4; i++) {
            partitionInfos[i] = new PartitionInfo(version, i, PEER1);
        }
        for (int i = 4; i < 8; i++) {
            partitionInfos[i] = new PartitionInfo(version, i, PEER2);
        }
        for (int i = 8; i < 12; i++) {
            partitionInfos[i] = new PartitionInfo(version, i, PEER3);
        }
        PartitionBalancingInfo partitionBalancingInfo = new PartitionBalancingInfo(partitionInfos);

        peerToBalancingInfo.put(PEER1, newBasicState(new PartitionBalancingInfo(PEER1, partitionBalancingInfo)));
        peerToBalancingInfo.put(PEER2, newBasicState(new PartitionBalancingInfo(PEER2, partitionBalancingInfo)));
 
        BasicEvenBalancer balancer = new BasicEvenBalancer(NB_PARTITIONS, peerToBalancingInfo);
        PartitionInfoUpdates infoUpdates = balancer.computePartitionInfoUpdates();
        PartitionInfoUpdate[] updates = infoUpdates.getPartitionUpdates();

        for (int i = 0; i < 8; i++) {
            assertFalse(updates[i].isRepopulate());
        }
        for (int i = 8; i < 12; i++) {
            assertTrue(updates[i].isRepopulate());
        }
    }
    
    private PartitionBalancingInfoState newBasicState(PartitionBalancingInfo balancingInfo) {
    	return new PartitionBalancingInfoState(false, balancingInfo);
    }
    
    private PartitionBalancingInfo balance(BasicEvenBalancer balancer) throws MessageExchangeException {
        PartitionInfoUpdates infoUpdates = balancer.computePartitionInfoUpdates();
        PartitionInfoUpdate[] updates = infoUpdates.getPartitionUpdates();
        PartitionInfo[] partitionInfos = new PartitionInfo[updates.length];
        for (int i = 0; i < updates.length; i++) {
            partitionInfos[i] = updates[i].getPartitionInfo();
        }
        return new PartitionBalancingInfo(partitionInfos);
    }

    private void assertVersion(int version, PartitionBalancingInfo resultingBalancingInfo) {
        PartitionInfo[] partitionInfos = resultingBalancingInfo.getPartitionInfos();
        for (int i = 0; i < partitionInfos.length; i++) {
            PartitionInfo partitionInfo = partitionInfos[i];
            assertEquals(version, partitionInfo.getVersion());
        }
    }

}