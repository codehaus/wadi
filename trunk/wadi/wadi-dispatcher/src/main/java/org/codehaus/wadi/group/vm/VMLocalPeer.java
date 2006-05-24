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
import java.util.Map;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;


/**
 * 
 * @version $Revision: 1603 $
 */
public class VMLocalPeer extends VMPeer implements LocalPeer, Serializable {

    public VMLocalPeer(String name) {
        super(name);
    }
    
    public void setState(Map newState) throws MessageExchangeException {
        synchronized (state) {
            state.clear();
            state.putAll(newState);
        }
    }
    
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
    
    public String toString() {
        return "Local Node :" + name;
    }
}