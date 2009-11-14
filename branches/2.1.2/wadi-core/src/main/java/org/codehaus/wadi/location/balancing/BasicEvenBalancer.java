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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1538 $
 */
class BasicEvenBalancer implements PartitionBalancingStrategy {
    private final Map<Peer, PartitionBalancingInfo> peerToBalancingInfo;
    private final int nbPeers;
    private final int nbPartitionPerPeer;
    private final int nbPartitions;
    private final BitSet lostPartitions;
    private final int version;
    private int nbSparePartitions;
    
    public BasicEvenBalancer(int nbPartitions, Map<Peer, PartitionBalancingInfoState> peerToBalancingInfoState) {
        this.nbPartitions = nbPartitions;

        Collection<PartitionBalancingInfoState> balancingStates = peerToBalancingInfoState.values();
        version = newBalancingVersion(balancingStates);
        lostPartitions = identifyLostPartitions(balancingStates);
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

    public PartitionInfoUpdates computePartitionInfoUpdates() throws MessageExchangeException {
        if (0 == nbPeers) {
            return new PartitionInfoUpdates(version, new PartitionInfoUpdate[0]);
        }
        
        PartitionInfoUpdateBuilder builder = new PartitionInfoUpdateBuilder(nbPartitions, version, lostPartitions);

        for (PartitionBalancingInfo balancingInfo : peerToBalancingInfo.values()) {
            balance(builder, balancingInfo);
        }

        if (nbSparePartitions > 0) {
            balanceSpare(builder);
        }

        if (0 != nbSparePartitions) {
            throw new AssertionError("nbSparePartitions should equal 0 at this stage.");
        }
        
        return builder.build();
    }

    protected int newBalancingVersion(Collection<PartitionBalancingInfoState> balancingStates) {
        int highestBalancingVersion = 0;
        for (PartitionBalancingInfoState balancingInfoState : balancingStates) {
            int version = balancingInfoState.getBalancingInfo().getHighestPartitionInfoVersion();
            if (version > highestBalancingVersion) {
                highestBalancingVersion = version;
            }
        }
        return ++highestBalancingVersion;
    }
    
    protected BitSet identifyLostPartitions(Collection<PartitionBalancingInfoState> balancingStates) {
        BitSet lostPartitions = new BitSet(nbPartitions);
        for (PartitionBalancingInfoState balancingInfoState : balancingStates) {
            PartitionInfo[] localPartitionInfos = balancingInfoState.getBalancingInfo().getLocalPartitionInfos();
            for (int i = 0; i < localPartitionInfos.length; i++) {
                PartitionInfo localPartitionInfo = localPartitionInfos[i];
                int index = localPartitionInfo.getIndex();
                lostPartitions.set(index);
            }
        }
        lostPartitions.flip(0, nbPartitions);
        return lostPartitions;
    }

    protected Map<Peer, PartitionBalancingInfo> filterOutEvacuatingState(Map<Peer, PartitionBalancingInfoState> peerToBalancingInfoState) {
        Map<Peer, PartitionBalancingInfo> peerToBalancingInfo = new HashMap<Peer, PartitionBalancingInfo>();
        for (Map.Entry<Peer, PartitionBalancingInfoState> entry : peerToBalancingInfoState.entrySet()) {
            Peer peer = entry.getKey();
            PartitionBalancingInfoState balancingInfoState = entry.getValue();
            if (balancingInfoState.isEvacuatingPartitions()) {
                continue;
            }
            peerToBalancingInfo.put(peer, balancingInfoState.getBalancingInfo());
        }
        return peerToBalancingInfo;
    }
    
    protected void balanceSpare(PartitionInfoUpdateBuilder balancingInfoBuilder) {
        for (Peer peer : peerToBalancingInfo.keySet()) {
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
        if (0 < nbPartitionToAdd) {
            balancingInfoBuilder.addPartitionInfos(balancingInfo, nbPartitionToAdd);
        } else {
            if (0 < nbSparePartitions) {
                nbSparePartitions--;
                nbPartitionToAdd++;
            }
            if (0 == nbPartitionToAdd) {
                balancingInfoBuilder.mergePartitionInfos(balancingInfo);
            } else if (0 < nbPartitionToAdd) {
                balancingInfoBuilder.addPartitionInfos(balancingInfo, nbPartitionToAdd);
            } else {
                balancingInfoBuilder.removePartitions(balancingInfo, -nbPartitionToAdd);
            }
        }
    }
}