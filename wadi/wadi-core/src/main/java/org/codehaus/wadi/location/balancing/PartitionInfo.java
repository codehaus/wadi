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

import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1538 $
 */
public class PartitionInfo implements Serializable {
    private Peer owner;
    private final int index;
    private final int version;
    private int numberOfExpectedMerge;
    private int numberOfCurrentMerge;

    public PartitionInfo(int version, int index) {
        if (version < 0) {
            throw new IllegalArgumentException("version must be >= 0");
        } else if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }
        this.version = version;
        this.index = index;
        
        owner = null;
    }
    
    public PartitionInfo(int version, int index, Peer owner) {
        if (version < 0) {
            throw new IllegalArgumentException("version must be >= 0");
        } else if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        } else if (null == owner) {
            throw new IllegalArgumentException("owner is required");
        }
        this.version = version;
        this.index = index;
        this.owner = owner;
    }

    public boolean isOwned() {
        return null != owner;
    }
    
    public Peer getOwner() {
        if (null == owner) {
            throw new IllegalStateException("No owner is defined");
        }
        return owner;
    }

    public int getIndex() {
        return index;
    }

    public int getVersion() {
        return version;
    }
    
    public int getNumberOfExpectedMerge() {
        return numberOfExpectedMerge;
    }

    public void setNumberOfExpectedMerge(int numberOfExpectedMerge) {
        if (numberOfExpectedMerge < 1) {
            throw new IllegalArgumentException("numberOfExpectedMerge must be greater than 0");
        }
        this.numberOfExpectedMerge = numberOfExpectedMerge;
    }
    
    public int getNumberOfCurrentMerge() {
        return numberOfCurrentMerge;
    }

    public void incrementNumberOfCurrentMerge() {
        numberOfCurrentMerge++;
    }

    public int hashCode() {
        return index * 37;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof PartitionInfo)) {
            return false;
        }
        PartitionInfo other = (PartitionInfo) obj;
        if (index != other.index) {
            return false;
        } else if (version != other.version) {
            return false;
        } else if (numberOfExpectedMerge != other.numberOfExpectedMerge) {
            return false;
        } else if (owner != other.owner) {
            return false;
        }
        return true;
    }
    
    public String toString() {
        return "Partition[" + index + "]; owned by [" + owner + "]; version [" + version + ".]; mergeVersion [" + numberOfExpectedMerge + "]";
    }

}
