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
package org.codehaus.wadi.dindex.impl;

import java.io.Serializable;

public class BucketRepopulateRequest implements Serializable {

    protected int[] _keys;
    
    public BucketRepopulateRequest(int[] keys) {
        super();
        _keys=keys;
    }
    
    protected BucketRepopulateRequest() {
        // for deserialisation...
    }

    int[] getKeys() {
        return _keys;
    }
}
