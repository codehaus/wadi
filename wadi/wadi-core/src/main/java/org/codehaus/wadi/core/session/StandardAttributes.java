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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;


/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1497 $
 */
public class StandardAttributes implements Attributes {
    protected final ValueFactory valueFactory;
    protected StandardAttributesMemento memento;

    public StandardAttributes(ValueFactory valueFactory) {
        if (null == valueFactory) {
            throw new IllegalArgumentException("valueFactory is required");
        }
        this.valueFactory = valueFactory;
        
        memento = newMemento();
    }

    protected StandardAttributesMemento newMemento() {
        return new StandardAttributesMemento();
    }

    protected Map<Object, Object> getAttributesMap() {
        return memento.getAttributes();
    }
    
    public synchronized Object get(Object key) {
        return getAttributesMap().get(key);
    }

    public synchronized Object remove(Object key) {
        return getAttributesMap().remove(key);
    }

    public synchronized Object put(Object key, Object newValue) {
        return getAttributesMap().put(key, newValue);
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
        return Collections.unmodifiableCollection(getAttributesMap().values());
    }

    public StandardAttributesMemento getMemento() {
        return memento;
    }

    public void setMemento(StandardAttributesMemento memento) {
        this.memento = memento;
    }
    
}
