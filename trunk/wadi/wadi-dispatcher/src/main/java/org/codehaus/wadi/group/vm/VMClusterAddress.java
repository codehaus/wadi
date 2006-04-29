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
package org.codehaus.wadi.group.vm;

import org.codehaus.wadi.group.Address;

/**
 * 
 * @version $Revision: 1603 $
 */
public class VMClusterAddress implements Address {
    private final VMCluster vmCluster;

    public VMClusterAddress(VMCluster vmCluster) {
        this.vmCluster = vmCluster;
    }

    public String getClusterName() {
        return vmCluster.getName();
    }

    public int hashCode() {
        return vmCluster.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (false == obj instanceof VMClusterAddress) {
            return false;
        }
        
        VMClusterAddress other = (VMClusterAddress) obj;
        return other.vmCluster == vmCluster;
    }
    
    public String toString() {
        return "Cluster Destination :" + vmCluster.getName();
    }
}
