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
package org.codehaus.wadi.activecluster;

import java.util.Map;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1603 $
 */
class ACNodeAdapter implements Peer {
    private final org.apache.activecluster.Node adaptee;
    private final Address address;
    
    public ACNodeAdapter(org.apache.activecluster.Node acNode) {
        this.adaptee = acNode;
        
        address = ACDestinationAdapter.wrap(adaptee.getDestination());
    }

    public Map getState() {
        return adaptee.getState();
    }

    public Address getAddress() {
        return address;
    }

    public String getName() {
        return adaptee.getName();
    }

    public org.apache.activecluster.Node getAdaptee() {
        return adaptee;
    }
    
    public boolean equals(Object obj) {
        if (false == obj instanceof ACNodeAdapter) {
            return false;
        }
        
        ACNodeAdapter other = (ACNodeAdapter) obj;
        return adaptee.equals(other.adaptee);
    }
}
