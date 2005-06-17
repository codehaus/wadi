/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.sandbox.dindex;

import java.io.Serializable;
import java.util.Map;

import javax.jms.ObjectMessage;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

public class LocalBucket extends AbstractBucket implements Serializable {

    protected Map _map=new ConcurrentHashMap();
    
    public LocalBucket(int key) {
        super(key);
    }
    
    protected LocalBucket() {
        super();
        // for deserialisation...
    }

    public boolean isLocal() {
        return true;
    }
    
    public String toString() {
        return "local";
    }
    
    public void dispatch(ObjectMessage om) {
        throw new UnsupportedOperationException(); // NYI
    }
}
