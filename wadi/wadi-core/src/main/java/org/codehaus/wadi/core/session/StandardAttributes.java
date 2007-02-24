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
package org.codehaus.wadi.core.session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.wadi.web.Attributes;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1497 $
 */
public class StandardAttributes implements Attributes {
    protected final Map attributes;
    protected final ValueFactory valueFactory;

    public StandardAttributes(ValueFactory valueFactory) {
        if (null == valueFactory) {
            throw new IllegalArgumentException("valueFactory is required");
        }
        this.valueFactory = valueFactory;
        
        attributes = new HashMap();
    }

    public synchronized Object get(Object key) {
        Value a = (Value) attributes.get(key);
        if (a == null) {
            return null;
        } else {
            return a.getValue();
        }
    }

    public synchronized Object remove(Object key) {
        Value a = (Value) attributes.remove(key);
        if (a == null) {
            return null;
        } else {
            return a.getValue();
        }
    }

    public synchronized Object put(Object key, Object newValue) {
        Value in = valueFactory.create();
        in.setValue(newValue);
        Value out = (Value) attributes.put(key, in);
        if (out == null) {
            return null;
        } else {
            return out.getValue();
        }
    }

    public synchronized int size() {
        return attributes.size();
    }

    public synchronized Set keySet() {
        return new HashSet(attributes.keySet());
    }

    public synchronized void clear() {
        attributes.clear();
    }

}
