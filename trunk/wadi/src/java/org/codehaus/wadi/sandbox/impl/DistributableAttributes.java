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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;

import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.AttributesConfig;
import org.codehaus.wadi.sandbox.DistributableAttributesConfig;
import org.codehaus.wadi.sandbox.DistributableValueConfig;
import org.codehaus.wadi.sandbox.ValueHelper;

/**
 * A DistributableAttributes object needs to be Listener aware. When a Session is invalidated in Serialised
 * state, we only want to deserialise the Attributes that we absolutely have to - in other words, those
 * expecting some kind of notification (activation or unbinding). If the Context has HttpSessionAttributeListeners
 * registered, then we will have to explicitly remove every attribute from every session anyway, so there is no need
 * to keep a separate tally.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DistributableAttributes extends StandardAttributes implements DistributableValueConfig {

    public DistributableAttributes(AttributesConfig config, Map map) {
        super(config, map);
    }
    
    protected Set _listenerNames=new HashSet();
    public Set getListenerNames() {return _listenerNames;}
    
    public Object remove(Object key) {
        Object oldValue=super.remove(key);
        // TODO - is it faster to test and fail, or to remove and fail ?
        // TODO - we could also check size of listener set ?
        if (isListener(oldValue))
            _listenerNames.remove(key);
        
        return oldValue;
    }
    
    public Object put(Object key, Object newValue) {
        Object oldValue=super.put(key, newValue);
        boolean wasListener=isListener(oldValue);
        boolean isListener=(newValue==oldValue)?wasListener:isListener(newValue);
        
        if (wasListener==isListener)
            return oldValue;
        
        if (wasListener)
            _listenerNames.remove(key);
        if (isListener)
            _listenerNames.add(key);
        
        return oldValue;
    }
    
    protected boolean isListener(Object o) {
        return (!((DistributableAttributesConfig)_config).getHttpSessionAttributeListenersRegistered() && // first test should not be done dynamically
        (o instanceof HttpSessionActivationListener || o instanceof HttpSessionBindingListener)); // TODO
    }

    public void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
        int size=oi.readInt();
        for (int i=0; i<size; i++) {
            Object key=oi.readObject();
            DistributableValue val=(DistributableValue)_config.getValuePool().take(this);
            val.readContent(oi);
            _map.put(key, val);
        }
    }

    public void writeContent(ObjectOutput oo) throws IOException {
        oo.writeInt(size());
        for (Iterator i=_map.entrySet().iterator(); i.hasNext();) {
            Map.Entry e=(Map.Entry)i.next();
            Object key=e.getKey();
            oo.writeObject(key);
            DistributableValue val=(DistributableValue)e.getValue();
            val.writeContent(oo);
        }
    }

    public ValueHelper findHelper(Class type) {return ((DistributableAttributesConfig)_config).findHelper(type);}

    public boolean getHttpSessionAttributeListenersRegistered() {return ((DistributableAttributesConfig)_config).getHttpSessionAttributeListenersRegistered();}

    public HttpSessionEvent getHttpSessionEvent() {return ((DistributableAttributesConfig)_config).getHttpSessionEvent();}
    
    public StreamingStrategy getStreamer(){return ((DistributableAttributesConfig)_config).getStreamer();}
    
}
