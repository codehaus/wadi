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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1538 $
 */
public class PartitionInfoUpdateBuilder {
    private final int version;
    private final BitSet lostPartitions;
    private final Map<Integer, Set<Peer>> partitionsToMerge;
    private final Collection<DeferredAdditionCommand> deferredAdditions;
    private final PartitionInfoUpdate[] partitionUpdates;
    
    public PartitionInfoUpdateBuilder(int nbPartitions, int version, BitSet lostPartitions) {
        if (1 > nbPartitions) {
            throw new IllegalArgumentException("nbPartitions must be greater than 0");
        } else if (0 > version) {
            throw new IllegalArgumentException("balacingVersion must be positive");
        } else if (null == lostPartitions) {
            throw new IllegalArgumentException("lostPartitions is required");
        }
        this.version = version;
        this.lostPartitions = lostPartitions;

        partitionsToMerge = new HashMap<Integer, Set<Peer>>();
        partitionUpdates = new PartitionInfoUpdate[nbPartitions];
        deferredAdditions = new ArrayList<DeferredAdditionCommand>();
    }
    
    public void addPartitionInfos(PartitionBalancingInfo baseline, int nbPartitionToAdd) {
        assertBaseline(baseline);
        if (nbPartitionToAdd < 1) {
            throw new IllegalArgumentException("nbPartitionToAdd must be greater than 0");
        }
        
        mergePartitionInfos(baseline);
        
        deferredAdditions.add(new DeferredAdditionCommand(baseline.getDefiningPeer(), nbPartitionToAdd));
    }

    public void mergePartitionInfos(PartitionBalancingInfo baseline) {
        assertBaseline(baseline);
        
        PartitionInfo[] localPartitionInfos = baseline.getLocalPartitionInfos();
        for (int i = 0; i < localPartitionInfos.length; i++) {
            PartitionInfo localPartitionInfo = localPartitionInfos[i];
            trackPartition(localPartitionInfo);
            derivePartitionInfoUpdate(localPartitionInfo);
        }
    }

    public void removePartitions(PartitionBalancingInfo baseline, int nbPartitionToRemove) {
        assertBaseline(baseline);
        if (nbPartitionToRemove < 1) {
            throw new IllegalArgumentException("nbPartitionToRemove must be greater than 0");
        }
        
        PartitionInfo[] localPartitionInfos = baseline.getLocalPartitionInfos();
        for (int i = 0; i < localPartitionInfos.length; i++) {
            PartitionInfo localPartitionInfo = localPartitionInfos[i];
            trackPartition(localPartitionInfo);
            if (nbPartitionToRemove > 0) {
                nbPartitionToRemove--;
            } else {
                derivePartitionInfoUpdate(localPartitionInfo);
            }
        }
        if (nbPartitionToRemove != 0) {
            throw new IllegalStateException("[" + nbPartitionToRemove + "] still to remove");
        }
    }

    public void addPartitionInfos(Peer peer, int nbPartitionToAdd) {
        if (nbPartitionToAdd < 1) {
            throw new IllegalArgumentException("nbPartitionToAdd must be greater than 0");
        }
        deferredAdditions.add(new DeferredAdditionCommand(peer, nbPartitionToAdd));
    }

    public PartitionInfoUpdates build() {
        for (DeferredAdditionCommand command : deferredAdditions) {
            allocatePartitionToPeer(command.peer, command.nbPartitionToAdd);
        }
        
        if (!areAllPartitionInfoOwned()) {
            throw new IllegalStateException("All partitions are not owned");
        }

        for (Map.Entry<Integer, Set<Peer>> entry : partitionsToMerge.entrySet()) {
            Integer index = entry.getKey();
            Set<Peer> peers = entry.getValue();
            if (peers.size() != 1) {
                PartitionInfoUpdate partitionInfoUpdate = partitionUpdates[index];
                peers.remove(partitionInfoUpdate.getPartitionInfo().getOwner());
                partitionInfoUpdate.getPartitionInfo().setNumberOfExpectedMerge(peers.size());
            }
        }
        
        return new PartitionInfoUpdates(version, partitionUpdates);
    }

    protected void trackPartition(PartitionInfo localPartitionInfo) {
        Set<Peer> peers = partitionsToMerge.get(localPartitionInfo.getIndex());
        if (null == peers) {
            peers = new HashSet<Peer>();
            partitionsToMerge.put(localPartitionInfo.getIndex(), peers);
        }
        peers.add(localPartitionInfo.getOwner());
    }

    protected void derivePartitionInfoUpdate(PartitionInfo localPartitionInfo) {
        int index = localPartitionInfo.getIndex();
        PartitionInfoUpdate partitionInfoUpdate = partitionUpdates[index];
        if (null == partitionInfoUpdate) {
            localPartitionInfo = newPartitionInfo(localPartitionInfo);
            partitionUpdates[index] = new PartitionInfoUpdate(lostPartitions.get(index), localPartitionInfo);
        }
    }

    protected void allocatePartitionToPeer(Peer peer, int nbPartitionToAdd) {
        for (int i = 0; i < partitionUpdates.length; i++) {
            PartitionInfoUpdate partitionInfoUpdate = partitionUpdates[i];
            if (null != partitionInfoUpdate) {
                continue;
            }
            partitionUpdates[i] = new PartitionInfoUpdate(lostPartitions.get(i), new PartitionInfo(version, i, peer));
            nbPartitionToAdd--;
            if (0 == nbPartitionToAdd) {
                break;
            }
        }
        if (0 != nbPartitionToAdd) {
            throw new IllegalStateException("[" + nbPartitionToAdd + "] still need to be added");
        }
    }

    public int getNumberOfPartitionsOwnedBy(Peer peer) {
        if (null == peer) {
            throw new IllegalArgumentException("peer is required");
        }
        int owned = 0;
        for (int i = 0; i < partitionUpdates.length; i++) {
            PartitionInfoUpdate partitionInfoUpdate = partitionUpdates[i];
            if (null != partitionInfoUpdate && peer.equals(partitionInfoUpdate.getPartitionInfo().getOwner())) {
                owned++;
            }
        }
        for (DeferredAdditionCommand command : deferredAdditions) {
            if (command.peer.equals(peer)) {
                owned += command.nbPartitionToAdd;
            }
        }
        return owned;
    }

    protected PartitionInfo newPartitionInfo(PartitionInfo localPartitionInfo) {
        return new PartitionInfo(version, localPartitionInfo.getIndex(), localPartitionInfo.getOwner());
    }
    
    private boolean areAllPartitionInfoOwned() {
        for (int i = 0; i < partitionUpdates.length; i++) {
            PartitionInfoUpdate partitionInfoUpdate = partitionUpdates[i];
            if (null == partitionInfoUpdate) {
                return false;
            }
        }
        return true;
    }
    
    private void assertBaseline(PartitionBalancingInfo baseline) {
        if (null == baseline) {
            throw new IllegalArgumentException("baseline is required");
        } else if (null == baseline.getDefiningPeer()) {
            throw new IllegalArgumentException("baseline does not define a definingPeer");
        } else if (partitionUpdates.length != baseline.getPartitionInfos().length) {
            throw new IllegalArgumentException("Cannot merge partition balancing info as its size [" + 
                    baseline.getPartitionInfos().length + "] does not equal the size of the target partition balancing " +
                    "info [" + partitionUpdates.length + "]");
        }
    }
    
    private static class DeferredAdditionCommand {
        private final Peer peer;
        private final int nbPartitionToAdd;
        
        public DeferredAdditionCommand(Peer peer, int nbPartitionToAdd) {
            this.peer = peer;
            this.nbPartitionToAdd = nbPartitionToAdd;
        }
    }
}
