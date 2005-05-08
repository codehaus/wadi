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
package org.codehaus.wadi.impl;

import java.io.IOException;
import java.io.NotSerializableException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Attributes;
import org.codehaus.wadi.AttributesConfig;
import org.codehaus.wadi.Session;
import org.codehaus.wadi.SessionConfig;
import org.codehaus.wadi.ValuePool;

/**
 * Our internal representation of any Web Session
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class StandardSession extends AbstractContext implements Session, AttributesConfig {
    
    protected final static Log _log = LogFactory.getLog(StandardSession.class);
    protected final SessionConfig _config;
    protected final Attributes _attributes;
    protected final HttpSession _wrapper;
    protected final HttpSessionEvent _httpSessionEvent;
    
    protected String _id; // like name, but possibly augmented with routing info...

    public StandardSession(SessionConfig config) {
        super();
        _config=config;
        _attributes=_config.getAttributesPool().take(this);
        _wrapper=_config.getSessionWrapperFactory().create(this);
        _httpSessionEvent=new HttpSessionEvent(_wrapper);
        
        // TODO - resolve different APIs used by Factories and Pools...
        }
    
    public void init(long creationTime, long lastAccessedTime, int maxInactiveInterval, String name) {
        super.init(creationTime, lastAccessedTime, maxInactiveInterval, name);
        _id=_config.getRouter().augment(_name);
    }

    
    public void destroy() {
        super.destroy();
        _attributes.clear();
        // NYI - other fields...
    }
    
    public byte[] getBytes() throws Exception {
        throw new NotSerializableException();
    }

    public void setBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        throw new NotSerializableException();
    }
    
    // public access to the contents of this session should all be directed via wrapper
    public HttpSession getWrapper() {return _wrapper;}
    
    // cached events...
    public HttpSessionEvent getHttpSessionEvent(){return _httpSessionEvent;}
    
    //public String getRealId() {return null;} // TODO - lose this method...
    
    // useful constants...
    protected static final String[]    _emptyStringArray =new String[0];
    protected static final Enumeration _emptyEnumeration =Collections.enumeration(Collections.EMPTY_LIST);
    
    // Attributes...
    
    // Setters
    
    public Object getAttribute(String name) {
        return _attributes.get(name);
    }
    
    public Set getAttributeNameSet() {
        return _attributes.keySet();
    }
    
    public Enumeration getAttributeNameEnumeration() {
        return _attributes.size()==0?_emptyEnumeration:Collections.enumeration(_attributes.keySet());
    }
    
    public String[] getAttributeNameStringArray() {
        return _attributes.size()==0?_emptyStringArray:(String[])_attributes.keySet().toArray(new String[_attributes.size()]);
    }
    
    public Object setAttribute(String name, Object value) {
        // we can be sure that name is non-null, because this will have
        // been checked in our facade...
        return _attributes.put(name, value);
    }
    
    public Object removeAttribute(String name) {
        // we could remove the Map if num entries fell back to '0' -
        // but we would probably be creating more work than saving
        // memory..
        return _attributes.remove(name);
    }
    
    public SessionConfig getConfig() {return _config;}
    
    // AttributesConfig
    
    public ValuePool getValuePool(){return _config.getValuePool();}
    
    // if I don't actually have a method in this class, I cannot advise it, without runtime overhead...
    // is this correct ?
    public void setLastAccessedTime(long lastAccessedTime) {super.setLastAccessedTime(lastAccessedTime);}
    public void setMaxInactiveInterval(int maxInactiveInterval) {super.setMaxInactiveInterval(maxInactiveInterval);}
    
    public String getId() {return _id;}
    
}
