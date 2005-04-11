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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.ValuePool;
import org.codehaus.wadi.sandbox.Attributes;
import org.codehaus.wadi.sandbox.AttributesConfig;
import org.codehaus.wadi.sandbox.SessionConfig;

/**
 * Our internal representation of any Web Session
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class Session extends AbstractContext implements AttributesConfig {
    
    protected final static Log _log = LogFactory.getLog(Session.class);
    protected final Attributes _attributes;
    protected final SessionConfig _config;
    
    public Session(SessionConfig config) {
        super();
        _config=config;
        _attributes=_config.getAttributesPool().take(this);
    }
    
    public void init(long creationTime, long lastAccessedTime, int maxInactiveInterval, boolean invalidated, String id, RWLock lock, Attributes attributes) {
        init(creationTime, lastAccessedTime, maxInactiveInterval, invalidated, id, lock);
        // _attributes=attributes; // FIXME
    }
    
    public void destroy() {
        _attributes.clear();
    }
    
    public void readContent(ObjectInput oi) throws IOException, ClassNotFoundException {
        // NYI
    }
    
    public void writeContent(ObjectOutput oo) throws IOException {
        // NYI
    }
    
    public void tidy() { // TODO - merge with destroy()
        super.tidy();
        _attributes.clear();
    }
    
    // public access to the contents of this session should all be directed via wrapper
    protected final HttpSession _wrapper=new SessionWrapper(this);
    public HttpSession getWrapper() {return _wrapper;}
    
    // cached events...
    protected final HttpSessionEvent _httpSessionEvent=new HttpSessionEvent(_wrapper);
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
    
    public Set getBindingListenerNames() {return _attributes.getBindingListenerNames();}
    public Set getActivationListenerNames() {return _attributes.getActivationListenerNames();}
    
    public Attributes getAttributes() {return _attributes;}
    public SessionConfig getConfig() {return _config;}
    
    // AttributesConfig
    
    public Session getSession(){return this;}
    public boolean hasListeners(){return false;} //NYI
    public ValuePool getAttributePool(){return _config.getAttributePool();}

}
