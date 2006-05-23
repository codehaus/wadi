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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import org.codehaus.wadi.group.Address;

/**
 * 
 * @version $Revision: 1603 $
 */
public class VMAddress implements Address {
    private final VMPeer node;

    public VMAddress(VMPeer node) {
        this.node = node;
    }

    public String getNodeName() {
        return node.getName();
    }

    public boolean equals(Object obj) {
        if (false == obj instanceof VMAddress) {
            return false;
        }
        
        VMAddress other = (VMAddress) obj;
        return node.equals(other.node);
    }
    
    public String toString() {
        return "Address [" + node.getName() + "]";
    }

    public Object writeReplace() throws ObjectStreamException {
        return new VMAddressInfo(node.getName());
    };
}
