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
package org.codehaus.wadi.impl;

import org.codehaus.wadi.Attributes;
import org.codehaus.wadi.AttributesConfig;
import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.AttributesPool;

public class SimpleAttributesPool implements AttributesPool {

    protected final AttributesFactory _factory;
    
    public SimpleAttributesPool(AttributesFactory factory) {
        super();
        _factory=factory;
    }

    public Attributes take(AttributesConfig config) {
        return _factory.create(config);
    }

    public void put(Attributes attributes) {
        // just drop Attributes...
    }

}
