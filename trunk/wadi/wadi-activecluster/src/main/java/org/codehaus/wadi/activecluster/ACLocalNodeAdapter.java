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

import javax.jms.JMSException;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;

/**
 * 
 * @version $Revision: 1603 $
 */
class ACLocalNodeAdapter implements LocalPeer {
    private final org.apache.activecluster.LocalNode adaptee;
    private final Address address;
    
    public ACLocalNodeAdapter(org.apache.activecluster.LocalNode adaptee) {
        this.adaptee = adaptee;
        
        address = new ACDestinationAdapter(adaptee.getDestination());
    }

    public Map getState() {
        return adaptee.getState();
    }

    public void setState(Map state) throws MessageExchangeException {
        try {
            adaptee.setState(state);
        } catch (JMSException e) {
            throw new MessageExchangeException(e);
        }
    }

    public Address getAddress() {
        return address;
    }

    public String getName() {
        return adaptee.getName();
    }

    public org.apache.activecluster.LocalNode getAdaptee() {
        return adaptee;
    }
    
    public boolean equals(Object obj) {
        if (false == obj instanceof ACLocalNodeAdapter) {
            return false;
        }
        
        ACLocalNodeAdapter other = (ACLocalNodeAdapter) obj;
        return adaptee.equals(other.adaptee);
    }
    
    public int hashCode() {
        return adaptee.hashCode();
    }
}
