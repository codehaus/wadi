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
package org.codehaus.wadi.sandbox.distributable.impl;

import org.codehaus.wadi.sandbox.Attributes;
import org.codehaus.wadi.sandbox.distributable.Distributer;

/**
 * Manage Attributes such that Object identity is scoped at a per-attribute
 * granularity. 
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class PartDistributer implements Distributer {

    public Attributes wrap(Attributes attributes) {
        return attributes;
    }

    public Object getAttribute(String id, Attributes attributes, String name) {
        return attributes.get(name);
    }
    
    public Object setAttribute(String id, Attributes attributes, String name, Object newValue) {
        return attributes.put(name, newValue);
    }
    
    public Object removeAttribute(String id, Attributes attributes, String name) {
        return attributes.remove(name);
    }

}
