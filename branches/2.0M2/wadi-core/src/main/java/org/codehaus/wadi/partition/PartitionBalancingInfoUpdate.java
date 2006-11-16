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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1538 $
 */
public class PartitionBalancingInfoUpdate implements Serializable {
    private final int version;
    private final PartitionInfoUpdate[] balancingInfoUpdates;
    private final boolean isPartitionManagerAlone;
    private final boolean partitionEvacuationAck;

    public PartitionBalancingInfoUpdate(int version,
            PartitionInfoUpdate[] balancingInfoUpdates,
            boolean isPartitionManagerAlone,
            boolean partitionEvacuationAck) {
        if (0 > version) {
            throw new IllegalArgumentException("version must be >= 0");
        } else if (null == balancingInfoUpdates) {
            throw new IllegalArgumentException("balancingInfoUpdates is required");
        }
        this.version = version;
        this.balancingInfoUpdates = balancingInfoUpdates;
        this.isPartitionManagerAlone = isPartitionManagerAlone;
        this.partitionEvacuationAck = partitionEvacuationAck;
    }

    public PartitionInfoUpdate[] getBalancingInfoUpdates() {
        return balancingInfoUpdates;
    }

    public boolean isPartitionEvacuationAck() {
        return partitionEvacuationAck;
    }

    public boolean isPartitionManagerAlone() {
        return isPartitionManagerAlone;
    }
    
    public PartitionBalancingInfo buildNewPartitionInfo(Peer peer) {
        PartitionInfo[] partitionInfos = new PartitionInfo[balancingInfoUpdates.length];
        for (int i = 0; i < balancingInfoUpdates.length; i++) {
            partitionInfos[i] = balancingInfoUpdates[i].getPartitionInfo();
        }
        PartitionBalancingInfo newBalancingInfo = new PartitionBalancingInfo(version, partitionInfos);
        newBalancingInfo = new PartitionBalancingInfo(peer, newBalancingInfo);
        return newBalancingInfo;
    }
    
    public boolean isRepopulationRequested() {
        return getIndicesToRepopulate().length > 0;
    }
    
    public int[] getIndicesToRepopulate() {
        List indexOfPartitionsToRepopulate = new ArrayList();
        for (int i = 0; i < balancingInfoUpdates.length; i++) {
            PartitionInfoUpdate partitionInfoUpdate = balancingInfoUpdates[i];
            if (partitionInfoUpdate.isRepopulate()) {
                indexOfPartitionsToRepopulate.add(new Integer(i));
            }
        }

        int[] returnedIndexes = new int[indexOfPartitionsToRepopulate.size()];
        for (int i = 0; i < returnedIndexes.length; i++) {
            returnedIndexes[i] = ((Integer) indexOfPartitionsToRepopulate.get(i)).intValue();
        }
        return returnedIndexes;
    }
    
}
