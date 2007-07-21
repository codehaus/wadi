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

    public StandardSession(Attributes attributes, Manager manager) {
        if (null == attributes) {
            throw new IllegalArgumentException("attributes is required");
        } else if (null == manager) {
            throw new IllegalArgumentException("manager is required");
        }
        this.manager = manager;
        
        getStandardSessionMemento().setAttributes(attributes);
    }

    @Override
    protected SimpleEvictableMemento newMemento() {
        return new StandardSessionMemento();
    }
    
    protected StandardSessionMemento getStandardSessionMemento() {
        return (StandardSessionMemento) memento;
    }
    
    protected Attributes getAttributes() {
        return getStandardSessionMemento().getAttributes();
    }
    
    public synchronized void destroy() throws Exception {
        manager.destroy(this);
        getAttributes().clear();
        onDestroy();
    }

    protected synchronized void destroyForMotion() throws Exception {
        getAttributes().clear();
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
        Object oldValue = getAttributes().put(key, value);
        onAddSate(key, oldValue, value);
        return oldValue;
    }

    public synchronized Object getState(Object key) {
        return getAttributes().get(key);
    }

    public synchronized Object removeState(Object key) {
        Object oldValue = getAttributes().remove(key);
        onRemoveState(key, oldValue);
        return oldValue;
    }
    
    public Map getState() {
        return new StateMap();
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
            for (Iterator iter = getAttributes().keySet().iterator(); iter.hasNext();) {
                Object key = (Object) iter.next();
                removeState(key);
            } 
        }

        public boolean containsKey(Object key) {
            return getAttributes().containsKey(key);
        }

        public boolean isEmpty() {
            return getAttributes().isEmpty();
        }

        public Set keySet() {
            return getAttributes().keySet();
        }

        public int size() {
            return getAttributes().size();
        }

        public Collection values() {
            return getAttributes().values();
        }

        public boolean containsValue(Object value) {
            return values().contains(value);
        }
        
        public Set entrySet() {
            throw new UnsupportedOperationException();
        }
    }

}
