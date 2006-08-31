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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;

import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1538 $
 */
public class PartitionInfoUpdateBuilder {
    private final PartitionInfoUpdate[] partitionInfoUpdates;
    private Collection deferredAdditions;
    private BitSet lostPartitions;
    
    public PartitionInfoUpdateBuilder(int nbPartitions) {
        if (1 > nbPartitions) {
            throw new IllegalArgumentException("nbPartitions must be greater than 0");
        }
        partitionInfoUpdates = new PartitionInfoUpdate[nbPartitions];
        for (int i = 0; i < partitionInfoUpdates.length; i++) {
            partitionInfoUpdates[i] = new PartitionInfoUpdate(false, new PartitionInfo(i));
        }
        
        deferredAdditions = new ArrayList();
        lostPartitions = new BitSet(nbPartitions);
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
            int index = localPartitionInfo.getIndex();
            PartitionInfoUpdate partitionInfoUpdate = partitionInfoUpdates[index];
            if (partitionInfoUpdate.getPartitionInfo().isOwned()) {
                throw new IllegalStateException("partition [" + partitionInfoUpdate + "] cannot be redefined");
            } else {
                partitionInfoUpdates[index] = new PartitionInfoUpdate(lostPartitions.get(index), localPartitionInfo);
            }
        }
    }

    public void removePartitions(PartitionBalancingInfo baseline, int nbPartitionToRemove) {
        assertBaseline(baseline);
        if (nbPartitionToRemove < 1) {
            throw new IllegalArgumentException("nbPartitionToRemove must be greater than 0");
        }
        
        PartitionInfo[] localPartitionInfos = baseline.getLocalPartitionInfos();
        for (int i = 0; i < localPartitionInfos.length; i++) {
            PartitionInfo localPartitionInfo = (PartitionInfo) localPartitionInfos[i];
            if (nbPartitionToRemove > 0) {
                nbPartitionToRemove--;
            } else {
                int index = localPartitionInfo.getIndex();
                PartitionInfoUpdate partitionInfoUpdate = partitionInfoUpdates[index];
                if (partitionInfoUpdate.getPartitionInfo().isOwned()) {
                    throw new IllegalStateException("partition [" + partitionInfoUpdate + "] cannot be redefined");
                } else {
                    partitionInfoUpdates[index] = new PartitionInfoUpdate(lostPartitions.get(index), localPartitionInfo);
                }
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

    public PartitionInfoUpdate[] build() {
        for (Iterator iter = deferredAdditions.iterator(); iter.hasNext();) {
            DeferredAdditionCommand additionCommand = (DeferredAdditionCommand) iter.next();
            
            int nbPartitionToAdd = additionCommand.nbPartitionToAdd;
            for (int i = 0; i < partitionInfoUpdates.length; i++) {
                PartitionInfoUpdate partitionInfoUpdate = partitionInfoUpdates[i];
                if (partitionInfoUpdate.getPartitionInfo().isOwned()) {
                    continue;
                }
                partitionInfoUpdates[i] = 
                    new PartitionInfoUpdate(lostPartitions.get(i), new PartitionInfo(i, additionCommand.peer));
                nbPartitionToAdd--;
                if (0 == nbPartitionToAdd) {
                    break;
                }
            }
            if (0 != nbPartitionToAdd) {
                throw new IllegalStateException("[" + nbPartitionToAdd + "] still need to be added");
            }
        }
        
        if (!areAllPartitionInfoOwned()) {
            throw new IllegalStateException("All partitions are not owned");
        }
        
        return partitionInfoUpdates;
    }

    public int getNumberOfPartitionsOwnedBy(Peer peer) {
        if (null == peer) {
            throw new IllegalArgumentException("peer is required");
        }
        int owned = 0;
        for (int i = 0; i < partitionInfoUpdates.length; i++) {
            PartitionInfoUpdate partitionInfoUpdate = partitionInfoUpdates[i];
            if (peer.equals(partitionInfoUpdate.getPartitionInfo().getOwner())) {
                owned++;
            }
        }
        for (Iterator iter = deferredAdditions.iterator(); iter.hasNext();) {
            DeferredAdditionCommand command = (DeferredAdditionCommand) iter.next();
            if (command.peer.equals(peer)) {
                owned += command.nbPartitionToAdd;
            }
        }
        return owned;
    }

    public void setLostPartitions(BitSet lostPartitions) {
        if (null == lostPartitions) {
            throw new IllegalArgumentException("lostPartitions is required");
        }
        this.lostPartitions = lostPartitions;
    }
    
    private boolean areAllPartitionInfoOwned() {
        for (int i = 0; i < partitionInfoUpdates.length; i++) {
            PartitionInfoUpdate partitionInfoUpdate = partitionInfoUpdates[i];
            if (!partitionInfoUpdate.getPartitionInfo().isOwned()) {
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
        } else if (partitionInfoUpdates.length != baseline.getPartitionInfos().length) {
            throw new IllegalArgumentException("Cannot merge partition balancing info as its size [" + 
                    baseline.getPartitionInfos().length + "] does not equal the size of the target partition balancing " +
                    "info [" + partitionInfoUpdates.length + "]");
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
