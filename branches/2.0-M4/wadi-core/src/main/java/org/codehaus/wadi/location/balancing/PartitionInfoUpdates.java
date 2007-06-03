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


/**
 * 
 * @version $Revision: 1538 $
 */
public class PartitionInfoUpdates {
    private final int version;
    private final PartitionInfoUpdate[] partitionUpdates;

    public PartitionInfoUpdates(int version, PartitionInfoUpdate[] partitionUpdates) {
        if (0 > version) {
            throw new IllegalArgumentException("version must be >= 0");
        } else if (null == partitionUpdates) {
                throw new IllegalArgumentException("partitionUpdates is required");
        }
        this.version = version;
        this.partitionUpdates = partitionUpdates;
    }

    public PartitionInfoUpdate[] getPartitionUpdates() {
        return partitionUpdates;
    }

    public int getVersion() {
        return version;
    }

}
