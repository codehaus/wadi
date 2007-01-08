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
package org.codehaus.wadi.web.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.wadi.Value;
import org.codehaus.wadi.ValueConfig;
import org.codehaus.wadi.web.Attributes;
import org.codehaus.wadi.web.AttributesConfig;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1497 $
 */
public class StandardAttributes implements Attributes, ValueConfig {
    protected final Map _map;
    protected final AttributesConfig _config;

    public StandardAttributes(AttributesConfig config, Map map) {
        _map = map;
        _config = config;
    }

    public synchronized Object get(Object key) {
        Value a = (Value) _map.get(key);
        if (a == null) {
            return null;
        } else {
            return a.getValue();
        }
    }

    public synchronized Object remove(Object key) {
        Value a = (Value) _map.remove(key);
        if (a == null) {
            return null;
        } else {
            Object tmp = a.getValue();
            a.setValue(null);
            _config.getValuePool().put(a);
            return tmp;
        }
    }

    public synchronized Object put(Object key, Object newValue) {
        Value in = _config.getValuePool().take(this);
        in.setValue(newValue);
        Value out = (Value) _map.put(key, in);
        if (out == null) {
            return null;
        } else {
            Object tmp = out.getValue();
            out.setValue(null);
            _config.getValuePool().put(out);
            return tmp;
        }
    }

    public synchronized int size() {
        return _map.size();
    }

    public synchronized Set keySet() {
        return new HashSet(_map.keySet());
    }

    public synchronized void clear() {
        _map.clear();
    }

}
