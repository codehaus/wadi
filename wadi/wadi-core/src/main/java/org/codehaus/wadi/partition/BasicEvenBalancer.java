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

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1538 $
 */
class BasicEvenBalancer implements PartitionBalancingStrategy {
    private final Map peerToBalancingInfo;
    private final int nbPeers;
    private final int nbPartitionPerPeer;
    private final int nbPartitions;
    private final BitSet lostPartitions;
    private final int version;
    private int nbSparePartitions;
    
    public BasicEvenBalancer(int nbPartitions, Map peerToBalancingInfoState) {
        this.nbPartitions = nbPartitions;

        version = newBalancingVersion(peerToBalancingInfoState.values());
        lostPartitions = identifyLostPartitions(peerToBalancingInfoState.values());
        peerToBalancingInfo = filterOutEvacuatingState(peerToBalancingInfoState);
        
        nbPeers = peerToBalancingInfo.size();
        if (0 == nbPeers) {
            nbPartitionPerPeer = 0;
            nbSparePartitions = 0;
        } else {
            nbPartitionPerPeer = nbPartitions / nbPeers;
            nbSparePartitions = nbPartitions % nbPeers;
        }
    }

    protected Map filterOutEvacuatingState(Map peerToBalancingInfoState) {
        Map peerToBalancingInfo = new HashMap();
        for (Iterator iter = peerToBalancingInfoState.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            Peer peer = (Peer) entry.getKey();
            PartitionBalancingInfoState balancingInfoState = (PartitionBalancingInfoState) entry.getValue();
            if (balancingInfoState.isEvacuatingPartitions()) {
                continue;
            }
            peerToBalancingInfo.put(peer, balancingInfoState.getBalancingInfo());
        }
        return peerToBalancingInfo;
    }
    
    protected int newBalancingVersion(Collection balancingStates) {
        int highestBalancingVersion = 0;
        for (Iterator iter = balancingStates.iterator(); iter.hasNext();) {
            PartitionBalancingInfoState balancingInfoState = (PartitionBalancingInfoState) iter.next();
            int version = balancingInfoState.getBalancingInfo().getHighestPartitionInfoVersion();
            if (version > highestBalancingVersion) {
                highestBalancingVersion = version;
            }
        }
        return ++highestBalancingVersion;
    }

    public PartitionInfoUpdates computePartitionInfoUpdates() throws MessageExchangeException {
        if (0 == nbPeers) {
            return new PartitionInfoUpdates(version, new PartitionInfoUpdate[0]);
        }
        
        PartitionInfoUpdateBuilder balancingInfoBuilder = new PartitionInfoUpdateBuilder(nbPartitions, version);
        balancingInfoBuilder.setLostPartitions(lostPartitions);

        for (Iterator iter = peerToBalancingInfo.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            PartitionBalancingInfo balancingInfo = (PartitionBalancingInfo) entry.getValue();
            balance(balancingInfoBuilder, balancingInfo);
        }

        if (nbSparePartitions > 0) {
            balanceSpare(balancingInfoBuilder);
        }

        if (0 != nbSparePartitions) {
            throw new AssertionError("nbSparePartitions should equal 0 at this stage.");
        }
        
        return balancingInfoBuilder.build();
    }

    protected BitSet identifyLostPartitions(Collection balancingStates) {
        BitSet lostPartitions = new BitSet(nbPartitions);
        for (Iterator iter = balancingStates.iterator(); iter.hasNext();) {
            PartitionBalancingInfoState balancingInfoState = (PartitionBalancingInfoState) iter.next();
            PartitionInfo[] localPartitionInfos = balancingInfoState.getBalancingInfo().getLocalPartitionInfos();
            for (int i = 0; i < localPartitionInfos.length; i++) {
                PartitionInfo localPartitionInfo = localPartitionInfos[i];
                int index = localPartitionInfo.getIndex();
                if (lostPartitions.get(index)) {
                    throw new IllegalStateException("Partition [" + localPartitionInfo + "] is already defined");
                } else {
                    lostPartitions.set(index);
                }
            }
        }
        lostPartitions.flip(0, nbPartitions);
        return lostPartitions;
    }
    
    protected void balanceSpare(PartitionInfoUpdateBuilder balancingInfoBuilder) {
        for (Iterator iter = peerToBalancingInfo.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            Peer peer = (Peer) entry.getKey();
            
            if (nbPartitionPerPeer == balancingInfoBuilder.getNumberOfPartitionsOwnedBy(peer)) {
                balancingInfoBuilder.addPartitionInfos(peer, 1);
                nbSparePartitions--;
                if (0 == nbSparePartitions) {
                    break;
                }
            }
        }
    }

    protected void balance(PartitionInfoUpdateBuilder balancingInfoBuilder, PartitionBalancingInfo balancingInfo) {
        int nbLocal = balancingInfo.getNumberOfLocalPartitionInfos();
        int nbPartitionToAdd = nbPartitionPerPeer - nbLocal;
        if (nbPartitionToAdd > 0) {
            balancingInfoBuilder.addPartitionInfos(balancingInfo, nbPartitionToAdd);
        } else {
            if (0 < nbSparePartitions) {
                nbSparePartitions--;
                nbPartitionToAdd++;
            }
            if (0 == nbPartitionToAdd) {
                balancingInfoBuilder.mergePartitionInfos(balancingInfo);
            } else {
                balancingInfoBuilder.removePartitions(balancingInfo, -nbPartitionToAdd);
            }
        }
    }
}