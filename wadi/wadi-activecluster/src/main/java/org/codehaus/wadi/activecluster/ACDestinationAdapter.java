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

import javax.jms.Destination;
import org.codehaus.wadi.group.Address;

/**
 * 
 * @version $Revision: 1603 $
 */
class ACDestinationAdapter implements Address {
    private static final long serialVersionUID = -4062450598175814658L;

    static Destination unwrap(Address address) {
        if (null == address) {
            return null;
        } else if (false == address instanceof ACDestinationAdapter) {
            throw new IllegalArgumentException("Expected " + 
                            ACDestinationAdapter.class.getName() +
                            ". Was:" + address.getClass().getName());
        }
        
        return ((ACDestinationAdapter) address).adaptee;
    }
    
    private final Destination adaptee;
    
    public ACDestinationAdapter(Destination adaptee) {
        this.adaptee = adaptee;
    }

    public Destination getAdaptee() {
        return adaptee;
    }
    
    public boolean equals(Object obj) {
        if (false == obj instanceof ACDestinationAdapter) {
            return false;
        }
        
        ACDestinationAdapter other = (ACDestinationAdapter) obj;
        return adaptee.equals(other.adaptee);
    }
    
    public int hashCode() {
        return adaptee.hashCode();
    }
}
