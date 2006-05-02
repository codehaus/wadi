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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Peer;


/**
 * 
 * @version $Revision: 1603 $
 */
public class VMPeer implements Peer, Serializable {
    protected final Map state = new HashMap();
    protected final String name;
    private final Address address;

    public VMPeer(String name) {
        this.name = name;
        address = new VMAddress(this);
    }
    
    public Map getState() {
        return state;
    }

    public Address getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }
    
    public boolean equals(Object obj) {
        if (false == obj instanceof VMPeer) {
            return false;
        }
        
        VMPeer other = (VMPeer) obj;
        return name.equals(other.name);
    }
    
    public String toString() {
        return "Node=[" + name + "]";
    }
}
