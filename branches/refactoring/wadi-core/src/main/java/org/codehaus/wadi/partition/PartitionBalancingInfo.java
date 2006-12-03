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
import java.util.Collection;

import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1538 $
 */
public class PartitionBalancingInfo implements Serializable {
    private final PartitionInfo[] partitionInfos;
    private final Peer definingPeer;
    
    public PartitionBalancingInfo(PartitionInfo[] partitionInfos) {
        if (null == partitionInfos) {
            throw new IllegalArgumentException("partitionInfos is required");
        } else if (!checkPartitionInfo(partitionInfos)) {
            throw new IllegalArgumentException("all the partitions are not owned");
        }
        this.partitionInfos = partitionInfos;
        
        definingPeer = null;
    }
    
    public PartitionBalancingInfo(Peer definingPeer, PartitionBalancingInfo prototype) {
        if (null == definingPeer) {
            throw new IllegalArgumentException("definingPeer is required");
        } else if (null == prototype) {
            throw new IllegalArgumentException("prototype is required");
        } else if (null != prototype.definingPeer) {
            throw new IllegalArgumentException("prototype should not have a definingPeer");
        }
        this.definingPeer = definingPeer;
        this.partitionInfos = prototype.partitionInfos;
    }

    public boolean isPrototype() {
        return null == definingPeer;
    }

    public Peer getDefiningPeer() {
        if (null == definingPeer) {
            throw new IllegalStateException("definingPeer is not set");
        }
        return definingPeer;
    }
    
    public int getNumberOfLocalPartitionInfos() {
        return getLocalPartitionInfos().length;
    }

    public PartitionInfo[] getLocalPartitionInfos() {
        return getPartitionsOwnedBy(definingPeer);
    }

    public int getNumberOfPartitionsOwnedBy(Peer peer) {
        return getPartitionsOwnedBy(peer).length;
    }

    public PartitionInfo[] getPartitionsOwnedBy(Peer peer) {
        if (null == peer) {
            throw new IllegalStateException("peer not set");
        }
        Collection foundPartitionInfos = new ArrayList();
        for (int i = 0; i < partitionInfos.length; i++) {
            PartitionInfo partitionInfo = partitionInfos[i];
            if (partitionInfo.isOwned() && peer.equals(partitionInfo.getOwner())) {
                foundPartitionInfos.add(partitionInfo);
            }
        }
        return (PartitionInfo[]) foundPartitionInfos.toArray(new PartitionInfo[0]);
    }
    
    public int getNumberOfPartitionInfos() {
        return partitionInfos.length;
    }
    
    public PartitionInfo[] getPartitionInfos() {
        PartitionInfo[] copy = new PartitionInfo[partitionInfos.length];
        System.arraycopy(partitionInfos, 0, copy, 0, copy.length);
        return copy;
    }
    
    public int getHighestPartitionInfoVersion() {
        int highestVersion = 0;
        for (int i = 0; i < partitionInfos.length; i++) {
            PartitionInfo partitionInfo = partitionInfos[i];
            if (highestVersion < partitionInfo.getVersion()) {
                highestVersion = partitionInfo.getVersion();
            }
        }
        return highestVersion;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer("Partition Balancing:size [" + partitionInfos.length + "]");
        for (int i = 0; i < partitionInfos.length; i++) {
            PartitionInfo partitionInfo = partitionInfos[i];
            buffer.append("; [" + partitionInfo + "]");
        }
        return buffer.toString();
    }
    
    private boolean checkPartitionInfo(PartitionInfo[] partitionInfos) {
        for (int i = 0; i < partitionInfos.length; i++) {
            PartitionInfo partitionInfo = partitionInfos[i];
            if (null == partitionInfo) {
                throw new IllegalArgumentException("partitionInfos[" + i + "] is null");
            } else if (partitionInfo.getIndex() != i) {
                throw new IllegalArgumentException("partitionInfos[" + i + "] has a wrong index");
            }
        }
        return true;
    }

}
