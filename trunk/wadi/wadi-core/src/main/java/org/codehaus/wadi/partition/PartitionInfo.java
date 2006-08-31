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

import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1538 $
 */
public class PartitionInfo implements Serializable {
    private final Peer owner;
    private final int index;

    public PartitionInfo(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index must be greater or equal to 0");
        }
        this.index = index;
        owner = null;
    }
    
    public PartitionInfo(int index, Peer owner) {
        if (index < 0) {
            throw new IllegalArgumentException("index must be greater or equal to 0");
        } else if (null == owner) {
            throw new IllegalArgumentException("owner is required");
        }
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
    
    public String toString() {
        return "Partition[" + index + "]; owned by [" + owner + "]";
    }
}
