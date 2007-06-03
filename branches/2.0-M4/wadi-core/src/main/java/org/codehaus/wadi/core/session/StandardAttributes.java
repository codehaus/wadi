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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1497 $
 */
public class StandardAttributes implements Attributes {
    protected final ValueFactory valueFactory;
    protected Map attributes;

    public StandardAttributes(ValueFactory valueFactory) {
        if (null == valueFactory) {
            throw new IllegalArgumentException("valueFactory is required");
        }
        this.valueFactory = valueFactory;
        
        attributes = newAttributes();
    }

    protected Map newAttributes() {
        return new HashMap();
    }
    
    protected Map getAttributesMap() {
        return attributes;
    }

    public synchronized Object get(Object key) {
        Value a = (Value) getAttributesMap().get(key);
        if (a == null) {
            return null;
        } else {
            return a.getValue();
        }
    }

    public synchronized Object remove(Object key) {
        Value a = (Value) getAttributesMap().remove(key);
        if (a == null) {
            return null;
        } else {
            return a.getValue();
        }
    }

    public synchronized Object put(Object key, Object newValue) {
        Value in = valueFactory.create();
        in.setValue(newValue);
        Value out = (Value) getAttributesMap().put(key, in);
        if (out == null) {
            return null;
        } else {
            return out.getValue();
        }
    }

    public synchronized void clear() {
        getAttributesMap().clear();
    }
    
    public synchronized boolean containsKey(Object key) {
        return getAttributesMap().containsKey(key);
    }
    
    public synchronized boolean isEmpty() {
        return getAttributesMap().isEmpty();
    }
    
    public synchronized Set keySet() {
        return getAttributesMap().keySet();
    }
    
    public synchronized int size() {
        return getAttributesMap().size();
    }

    public synchronized Collection values() {
        Collection values = new ArrayList();
        Map attributesMap = getAttributesMap();
        for (Iterator iter = attributesMap.values().iterator(); iter.hasNext();) {
            Value value = (Value) iter.next();
            values.add(value.getValue());
        }
        return values;
    }

}
