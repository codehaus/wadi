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

import java.io.IOException;
import java.io.NotSerializableException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.codehaus.wadi.core.eviction.SimpleEvictableMemento;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.motable.AbstractMotable;

/**
 * Our internal representation of any Web Session
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1886 $
 */
public class StandardSession extends AbstractMotable implements Session {
    protected final Manager manager;
    protected final StandardAttributes attributes;
    protected final Map<Object, Object> localStateMap;

    public StandardSession(StandardAttributes attributes, Manager manager) {
        if (null == attributes) {
            throw new IllegalArgumentException("attributes is required");
        } else if (null == manager) {
            throw new IllegalArgumentException("manager is required");
        }
        this.manager = manager;
        this.attributes = attributes;
        
        localStateMap = new HashMap<Object, Object>();
        
        getStandardSessionMemento().setAttributesMemento(attributes.getMemento());
    }

    @Override
    protected SimpleEvictableMemento newMemento() {
        return new StandardSessionMemento();
    }
    
    protected StandardSessionMemento getStandardSessionMemento() {
        return (StandardSessionMemento) memento;
    }
    
    public synchronized void destroy() throws Exception {
        manager.destroy(this);
        attributes.clear();
        onDestroy();
    }

    protected synchronized void destroyForMotion() throws Exception {
        attributes.clear();
    }

    public synchronized void onEndProcessing() {
        getAbstractMotableMemento().setNewSession(false);
    }
    
    public byte[] getBodyAsByteArray() throws Exception {
        throw new NotSerializableException();
    }

    public void setBodyAsByteArray(byte[] bytes) throws IOException, ClassNotFoundException {
        throw new NotSerializableException();
    }
    
    public synchronized Object addState(Object key, Object value) {
        Object oldValue = attributes.put(key, value);
        onAddSate(key, oldValue, value);
        return oldValue;
    }

    public synchronized Object getState(Object key) {
        return attributes.get(key);
    }

    public synchronized Object removeState(Object key) {
        Object oldValue = attributes.remove(key);
        onRemoveState(key, oldValue);
        return oldValue;
    }
    
    public Map getState() {
        return new StateMap();
    }
    
    public Map<Object, Object> getLocalStateMap() {
        return localStateMap;
    }

    protected void onAddSate(Object key, Object oldValue, Object newValue) {
    }

    protected void onRemoveState(Object key, Object oldValue) {
    }
    
    protected void onDestroy() {
    }

    protected class StateMap implements Map {
        
        public Object put(Object key, Object value) {
            return addState(key, value);
        }
        
        public void putAll(Map all) {
            for (Iterator iter = all.entrySet().iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();
                addState(entry.getKey(), entry.getValue());
            } 
        }
        
        public Object remove(Object key) {
            return removeState(key);
        }
        
        public Object get(Object key) {
            return getState(key);
        }

        public void clear() {
            for (Iterator iter = attributes.keySet().iterator(); iter.hasNext();) {
                Object key = (Object) iter.next();
                removeState(key);
            } 
        }

        public boolean containsKey(Object key) {
            return attributes.containsKey(key);
        }

        public boolean isEmpty() {
            return attributes.isEmpty();
        }

        public Set keySet() {
            return attributes.keySet();
        }

        public int size() {
            return attributes.size();
        }

        public Collection values() {
            return attributes.values();
        }

        public boolean containsValue(Object value) {
            return values().contains(value);
        }
        
        public Set entrySet() {
            throw new UnsupportedOperationException();
        }
    }

}
