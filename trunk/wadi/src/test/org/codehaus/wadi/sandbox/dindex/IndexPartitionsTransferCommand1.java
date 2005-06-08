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

import javax.jms.Destination;


public class IndexPartitionsTransferCommand1 implements Serializable {

    protected int _keep;
    protected Destination _target;
    
    public IndexPartitionsTransferCommand1(int keep, Destination target) {
        _keep=keep;
        _target=target;
    }
    
    protected IndexPartitionsTransferCommand1() {
        // for deserialisation...
    }
    
    public int getKeep() {
        return _keep;
    }
    
    public Destination getTarget() {
        return _target;
    }
}
