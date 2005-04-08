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
package org.codehaus.wadi.sandbox.impl;

import org.codehaus.wadi.sandbox.Attribute;
import org.codehaus.wadi.sandbox.AttributeConfig;
import org.codehaus.wadi.sandbox.AttributeFactory;
import org.codehaus.wadi.sandbox.AttributePool;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class SimpleAttributePool implements AttributePool {
    
    protected final AttributeFactory _factory;
    
    public SimpleAttributePool(AttributeFactory factory) {_factory=factory;}

    public Attribute take(AttributeConfig config) {
        return _factory.create(config);
    }

    public void put(Attribute attribute) {
        // just drop the Attribute - no pooling...
    }

}
