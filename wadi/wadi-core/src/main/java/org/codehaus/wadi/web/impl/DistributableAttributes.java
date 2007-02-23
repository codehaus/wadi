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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingListener;

import org.codehaus.wadi.ValueFactory;

/**
 * A DistributableAttributes object needs to be Listener aware. When a Session is invalidated in Serialised
 * state, we only want to deserialise the Attributes that we absolutely have to - in other words, those
 * expecting some kind of notification (activation or unbinding). If the Context has HttpSessionAttributeListeners
 * registered, then we will have to explicitly remove every attribute from every session anyway, so there is no need
 * to keep a separate tally.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1139 $
 */
public class DistributableAttributes extends StandardAttributes implements Externalizable {
    protected Set _listenerNames = new HashSet();
    
    public DistributableAttributes(ValueFactory valueFactory) {
        super(valueFactory);
    }

    public Set getListenerNames() {
        return _listenerNames;
    }

    public synchronized Object remove(Object key) {
        Object oldValue = super.remove(key);
        if (isListener(oldValue)) {
            _listenerNames.remove(key);
        }
        return oldValue;
    }

    public synchronized Object put(Object key, Object newValue) {
        Object oldValue = super.put(key, newValue);
        boolean wasListener = isListener(oldValue);
        boolean isListener = isListener(newValue);

        if (wasListener == isListener)
            return oldValue;

        if (wasListener)
            _listenerNames.remove(key);
        if (isListener)
            _listenerNames.add(key);

        return oldValue;
    }

    protected boolean isListener(Object o) {
        return o instanceof HttpSessionActivationListener || o instanceof HttpSessionBindingListener;
    }

    public synchronized void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {
        _listenerNames = (Set) oi.readObject();
        int size = oi.readInt();
        for (int i = 0; i < size; i++) {
            Object key = oi.readObject();
            DistributableValue val = (DistributableValue) valueFactory.create();
            val.readExternal(oi);
            attributes.put(key, val);
        }
    }

    public synchronized void writeExternal(ObjectOutput oo) throws IOException {
        oo.writeObject(_listenerNames);
        oo.writeInt(size());
        for (Iterator i = attributes.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            Object key = e.getKey();
            oo.writeObject(key);
            DistributableValue val = (DistributableValue) e.getValue();
            val.writeExternal(oo);
        }
    }

}
