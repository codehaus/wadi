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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingListener;

import org.codehaus.wadi.sandbox.AttributesConfig;
import org.codehaus.wadi.sandbox.DistributableAttributesConfig;

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
public class DistributableAttributes extends StandardAttributes {

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
        return (((DistributableAttributesConfig)_config).getHttpSessionAttributeListenersRegistered() && // first test should not be done dynamically
        (o instanceof HttpSessionActivationListener || o instanceof HttpSessionBindingListener)); // TODO
    }
    
}
