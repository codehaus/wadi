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

import org.codehaus.wadi.Manager;
import org.codehaus.wadi.impl.AbstractSession;
import org.codehaus.wadi.web.Attributes;
import org.codehaus.wadi.web.Router;
import org.codehaus.wadi.web.WADIHttpSession;
import org.codehaus.wadi.web.WebSessionConfig;
import org.codehaus.wadi.web.WebSessionWrapperFactory;

/**
 * Our internal representation of any Web Session
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1886 $
 */

public class StandardSession extends AbstractSession implements WADIHttpSession {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Enumeration EMPTY_ENUMERATION = Collections.enumeration(Collections.EMPTY_LIST);

    protected final WebSessionConfig config;
    protected final Router router;
    protected final HttpSession wrapper;
    protected final HttpSessionEvent httpSessionEvent;
    protected final Manager manager;

    public StandardSession(WebSessionConfig config,
            Attributes attributes,
            WebSessionWrapperFactory wrapperFactory,
            Router router,
            Manager manager) {
        super(attributes);
        if (null == wrapperFactory) {
            throw new IllegalArgumentException("wrapperFactory is required.");
        } else if (null == router) {
            throw new IllegalArgumentException("router is required.");
        } else if (null == manager) {
            throw new IllegalArgumentException("manager is required.");
        }
        this.config = config;
        this.router = router;
        this.manager = manager;
        
        wrapper = wrapperFactory.create(this);
        httpSessionEvent = new HttpSessionEvent(wrapper);
    }

    public synchronized void destroy() throws Exception {
        manager.destroy(this);
        super.destroy();
        attributes.clear();
    }

    public byte[] getBodyAsByteArray() throws Exception {
        throw new NotSerializableException();
    }

    public void setBodyAsByteArray(byte[] bytes) throws IOException, ClassNotFoundException {
        throw new NotSerializableException();
    }

    public HttpSession getWrapper() {
        return wrapper;
    }

    public HttpSessionEvent getHttpSessionEvent() {
        return httpSessionEvent;
    }

    public synchronized Object getAttribute(String name) {
        if (null == name) {
            throw new IllegalArgumentException("HttpSession attribute names must be non-null (see SRV.15.1.7.1)");
        }
        return attributes.get(name);
    }

    public synchronized Set getAttributeNameSet() {
        return attributes.keySet();
    }

    public synchronized Enumeration getAttributeNameEnumeration() {
        return attributes.size() == 0 ? EMPTY_ENUMERATION : Collections.enumeration(attributes.keySet());
    }

    public synchronized String[] getAttributeNameStringArray() {
        return attributes.size() == 0 ? EMPTY_STRING_ARRAY : (String[]) attributes.keySet()
                .toArray(new String[attributes.size()]);
    }

    public synchronized Object setAttribute(String name, Object newValue) {
        if (null == name) {
            throw new IllegalArgumentException("HttpSession attribute names must be non-null (see SRV.15.1.7.1)");
        }
        Object oldValue = attributes.put(name, newValue);
        onSetAttribute(name, oldValue, newValue);
        return oldValue;
    }

    public synchronized Object removeAttribute(String name) {
        if (null == name) {
            throw new IllegalArgumentException("HttpSession attribute names must be non-null (see SRV.15.1.7.1)");
        }
        Object oldValue = attributes.remove(name);
        onRemoveAttribute(name, oldValue);
        return oldValue;
    }

    public WebSessionConfig getConfig() {
        return config;
    }

    public String getId() {
        return router.augment(name);
    }
    
    protected synchronized void destroyForMotion() throws Exception {
        super.destroyForMotion();
        attributes.clear();
    }
    
    protected void onSetAttribute(String name, Object oldValue, Object newValue) {
        if (oldValue instanceof HttpSessionBindingListener) {
            ((HttpSessionBindingListener) oldValue).valueUnbound(new HttpSessionBindingEvent(wrapper, name, oldValue));
        }
        if (newValue instanceof HttpSessionBindingListener) {
            ((HttpSessionBindingListener) newValue).valueBound(new HttpSessionBindingEvent(wrapper, name, newValue));
        }

        boolean replaced = oldValue != null;
        HttpSessionAttributeListener[] listeners = config.getAttributeListeners();
        if (replaced) {
            HttpSessionBindingEvent hsbe = new HttpSessionBindingEvent(wrapper, name, oldValue);
            for (int i = 0; i < listeners.length; i++) {
                listeners[i].attributeReplaced(hsbe);
            }
        } else {
            HttpSessionBindingEvent hsbe = new HttpSessionBindingEvent(wrapper, name, newValue);
            for (int i = 0; i < listeners.length; i++) {
                listeners[i].attributeAdded(hsbe);
            }
        }
    }
    
    protected void onRemoveAttribute(String name, Object oldValue) {
        if (oldValue instanceof HttpSessionBindingListener) {
            ((HttpSessionBindingListener) oldValue).valueUnbound(new HttpSessionBindingEvent(wrapper, name, oldValue));
        }

        if (null != oldValue) {
            HttpSessionAttributeListener[] listeners = config.getAttributeListeners();
            HttpSessionBindingEvent hsbe = new HttpSessionBindingEvent(wrapper, name, oldValue);
            for (int i = 0; i < listeners.length; i++) {
                listeners[i].attributeRemoved(hsbe);
            }
        }
    }

}
