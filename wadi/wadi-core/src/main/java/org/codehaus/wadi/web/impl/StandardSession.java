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

import java.io.IOException;
import java.io.NotSerializableException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.impl.AbstractSession;
import org.codehaus.wadi.web.Attributes;
import org.codehaus.wadi.web.AttributesConfig;
import org.codehaus.wadi.web.WADIHttpSession;
import org.codehaus.wadi.web.WebSessionConfig;

/**
 * Our internal representation of any Web Session
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1886 $
 */

public class StandardSession extends AbstractSession implements WADIHttpSession, AttributesConfig {
	
	protected final static Log _log = LogFactory.getLog(StandardSession.class);
	protected final WebSessionConfig _config;
	protected final Attributes _attributes;
	protected final HttpSession _wrapper;
	protected final HttpSessionEvent _httpSessionEvent;
	
	public StandardSession(WebSessionConfig config) {
		super();
		_config=config;
		_attributes=_config.getAttributesFactory().create(this);
		_wrapper=_config.getSessionWrapperFactory().create(this);
		_httpSessionEvent=new HttpSessionEvent(_wrapper);
		
		// TODO - resolve different APIs used by Factories and Pools...
	}
	
	public void destroy() throws Exception {
		super.destroy();
		_attributes.clear();
		// NYI - other fields...
	}
	
	public byte[] getBodyAsByteArray() throws Exception {
		throw new NotSerializableException();
	}
	
	public void setBodyAsByteArray(byte[] bytes) throws IOException, ClassNotFoundException {
		throw new NotSerializableException();
	}
	
	// public access to the contents of this session should all be directed via wrapper
	public HttpSession getWrapper() {
		return _wrapper;
	}
	
	// cached events...
	public HttpSessionEvent getHttpSessionEvent() {
		return _httpSessionEvent;
	}
	
	//public String getRealId() {return null;} // TODO - lose this method...
	
	// useful constants...
	protected static final String[]    _emptyStringArray =new String[0];
	protected static final Enumeration _emptyEnumeration =Collections.enumeration(Collections.EMPTY_LIST);
	
	// Attributes...
	
	// Setters
	
	public Object getAttribute(String name) {
		if (null==name)
			throw new IllegalArgumentException("HttpSession attribute names must be non-null (see SRV.15.1.7.1)");
		
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
	
	protected void notifyBindingListeners(String name, Object oldValue, Object newValue) {
		if (null!=oldValue && oldValue instanceof HttpSessionBindingListener)
			((HttpSessionBindingListener)oldValue).valueUnbound(new HttpSessionBindingEvent(_wrapper, name, oldValue));
		if (newValue instanceof HttpSessionBindingListener)
			((HttpSessionBindingListener)newValue).valueBound(new HttpSessionBindingEvent(_wrapper, name, newValue));
	}
	
	protected void notifyAttributeListeners(String name, Object oldValue, Object newValue) {
		boolean replaced=(oldValue!=null);
		HttpSessionAttributeListener[] listeners=_config.getAttributeListeners();
		
		int l=listeners.length;
		if (l>0) {
			if (replaced) { // only test once, instead of inside the loop - results in duplicate code...
				HttpSessionBindingEvent hsbe=new HttpSessionBindingEvent(_wrapper, name, oldValue);
				for (int i=0; i<l; i++)
					listeners[i].attributeReplaced(hsbe);
			} else {
				HttpSessionBindingEvent hsbe=new HttpSessionBindingEvent(_wrapper, name, newValue);
				for (int i=0; i<l; i++)
					listeners[i].attributeAdded(hsbe);
			}
		}
	}
	
	public Object setAttribute(String name, Object newValue) {
		if (null==name)
			throw new IllegalArgumentException("HttpSession attribute names must be non-null (see SRV.15.1.7.1)");
		
		Object oldValue=_attributes.put(name, newValue);
		
		notifyBindingListeners(name, oldValue, newValue);
		notifyAttributeListeners(name, oldValue, newValue);
		
		return oldValue;
	}
	
	void notifyAttributeListeners(String name, Object oldValue) {
		if (null!=oldValue) {
			HttpSessionAttributeListener[] listeners=_config.getAttributeListeners();
			int l=listeners.length;
			if (l>0) {
				HttpSessionBindingEvent hsbe=new HttpSessionBindingEvent(_wrapper, name, oldValue);
				for (int i=0; i<l; i++)
					listeners[i].attributeRemoved(hsbe);
			}
		}
	}
	
	void notifyBindingListeners(String name, Object oldValue) {
		if (null!=oldValue && oldValue instanceof HttpSessionBindingListener) {
			((HttpSessionBindingListener)oldValue).valueUnbound(new HttpSessionBindingEvent(_wrapper, name, oldValue));
		}
	}
	
	public Object removeAttribute(String name) {
		if (null==name)
			throw new IllegalArgumentException("HttpSession attribute names must be non-null (see SRV.15.1.7.1)");
		// we could remove the Map if num entries fell back to '0' -
		// but we would probably be creating more work than saving
		// memory..
		Object oldValue=_attributes.remove(name);
		
		notifyBindingListeners(name, oldValue);
		notifyAttributeListeners(name, oldValue);
		
		return oldValue;
	}
	
	public WebSessionConfig getConfig() {
		return _config;
	}
	
	public ValuePool getValuePool(){
		return _config.getValuePool();
	}
	
	public String getId() {
		return _config.getRouter().augment(_name);
	}
	
}
