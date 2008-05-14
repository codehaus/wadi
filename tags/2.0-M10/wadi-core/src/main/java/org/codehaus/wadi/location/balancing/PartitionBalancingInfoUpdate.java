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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1538 $
 */
public class PartitionBalancingInfoUpdate implements Serializable {
    private final PartitionInfoUpdate[] balancingInfoUpdates;
    private final boolean isPartitionManagerAlone;
    private final boolean partitionEvacuationAck;

    public PartitionBalancingInfoUpdate(PartitionInfoUpdate[] balancingInfoUpdates,
            boolean isPartitionManagerAlone,
            boolean partitionEvacuationAck) {
        if (null == balancingInfoUpdates) {
            throw new IllegalArgumentException("balancingInfoUpdates is required");
        }
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
        PartitionBalancingInfo newBalancingInfo = new PartitionBalancingInfo(partitionInfos);
        return new PartitionBalancingInfo(peer, newBalancingInfo);
    }
    
    public PartitionInfoUpdate[] getRepopulatePartitionInfoUpdates() {
        List<PartitionInfoUpdate> partitionInfoUpdatesToRepopulate = new ArrayList<PartitionInfoUpdate>();
        for (int i = 0; i < balancingInfoUpdates.length; i++) {
            PartitionInfoUpdate partitionInfoUpdate = balancingInfoUpdates[i];
            if (partitionInfoUpdate.isRepopulate()) {
                partitionInfoUpdatesToRepopulate.add(partitionInfoUpdate);
            }
        }
        return partitionInfoUpdatesToRepopulate.toArray(new PartitionInfoUpdate[0]);
    }
    
}
