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

import org.codehaus.wadi.dindex.Partition;

public abstract class AbstractPartition implements Partition, Serializable {

    protected int _key;

    public AbstractPartition(int key) {
        _key=key;
    }

    protected AbstractPartition() {
        // for deserialisation...
    }

    public int getKey() {
        return _key;
    }

}