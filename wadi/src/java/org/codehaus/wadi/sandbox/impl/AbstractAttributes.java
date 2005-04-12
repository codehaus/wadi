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

import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSessionEvent;

import org.codehaus.wadi.SerializableContent;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.DistributableAttributesConfig;
import org.codehaus.wadi.sandbox.DistributableValueConfig;
import org.codehaus.wadi.sandbox.Session;
import org.codehaus.wadi.sandbox.Value;
import org.codehaus.wadi.sandbox.Attributes;
import org.codehaus.wadi.sandbox.AttributesConfig;

public abstract class AbstractAttributes implements Attributes, SerializableContent, DistributableValueConfig { // yeugh

    protected final Map _map;
    protected final AttributesConfig _config;
    
    public AbstractAttributes(AttributesConfig config, Map map) {
        super();
        _map=map; // could be inherited ?
        _config=config;
    }

    public Object get(Object key) {
        Value a=(Value)_map.get(key);
        if (a==null){
            return null;
        } else {
            return a.getValue();
        }
    }

    public Object remove(Object key) {
        Value a=(Value)_map.remove(key);
        if (a==null) {
            return null;
        } else {
            Object tmp=a.getValue();
            a.setValue(null);
            _config.getValuePool().put(a);
            return tmp;
        }
    }

    public Object put(Object key, Object newValue) {
        Value in=_config.getValuePool().take(this);
        in.setValue(newValue);
        Value out=(Value)_map.put(key, in);
        if (out==null) {
            return null;
        } else {
            Object tmp=out.getValue();
            out.setValue(null);
            _config.getValuePool().put(out);
            return tmp;
        }
    }

    public int size() {return _map.size();}
    public Set keySet() {return _map.keySet();}

    public void clear() {
        // should we null-out all Attributes - TODO
        _map.clear();
    }

    public byte[] getBytes() {return Utils.safeGetContent(this, getStreamer());}
    public void setBytes(byte[] bytes) {Utils.safeSetContent(this, bytes, getStreamer());}

    public Session getSession() {return _config.getSession();}
    
//    public Set getBindingListenerNames() {return _config.getSession().getBindingListenerNames();}
//    public Set getActivationListenerNames() {return _config.getSession().getActivationListenerNames();}

    public HttpSessionEvent getHttpSessionEvent() {return _config.getSession().getHttpSessionEvent();}

    // Distributable
    public StreamingStrategy getStreamer() {return ((DistributableAttributesConfig)_config).getStreamer();}

}
