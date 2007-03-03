/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.web;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;

import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.manager.Router;
import org.codehaus.wadi.core.session.AtomicallyReplicableSession;
import org.codehaus.wadi.core.session.Attributes;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.replication.Replicater;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicWebSession extends AtomicallyReplicableSession implements WADIHttpSession {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final Enumeration EMPTY_ENUMERATION = Collections.enumeration(Collections.EMPTY_LIST);

    protected final WebSessionConfig config;
    protected final Router router;
    protected final HttpSession wrapper;
    protected final HttpSessionEvent httpSessionEvent;

    public BasicWebSession(WebSessionConfig config,
            Attributes attributes,
            WebSessionWrapperFactory wrapperFactory,
            Router router,
            Manager manager,
            Streamer streamer,
            Replicater replicater) {
        super(attributes, manager, streamer, replicater);
        if (null == wrapperFactory) {
            throw new IllegalArgumentException("wrapperFactory is required.");
        } else if (null == router) {
            throw new IllegalArgumentException("router is required.");
        } else if (null == manager) {
            throw new IllegalArgumentException("manager is required.");
        }
        this.config = config;
        this.router = router;
        
        wrapper = wrapperFactory.create(this);
        httpSessionEvent = new HttpSessionEvent(wrapper);
    }

    public HttpSession getWrapper() {
        return wrapper;
    }

    public HttpSessionEvent getHttpSessionEvent() {
        return httpSessionEvent;
    }

    public WebSessionConfig getConfig() {
        return config;
    }

    public String getId() {
        return router.augment(name);
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
        return attributes.size() == 0 ? EMPTY_STRING_ARRAY : (String[]) attributes.keySet().toArray(new String[0]);
    }

    public synchronized Object setAttribute(String name, Object newValue) {
        if (null == name) {
            throw new IllegalArgumentException("HttpSession attribute names must be non-null (see SRV.15.1.7.1)");
        }
        return super.addState(name, newValue);
    }

    public synchronized Object removeAttribute(String name) {
        if (null == name) {
            throw new IllegalArgumentException("HttpSession attribute names must be non-null (see SRV.15.1.7.1)");
        }
        return super.removeState(name);
    }

    protected void onDeserialization() {
        for (Iterator iter = attributes.values().iterator(); iter.hasNext();) {
            Object value = iter.next();
            if (value instanceof HttpSessionActivationListener) {
                HttpSessionActivationListener listener = (HttpSessionActivationListener) value;
                listener.sessionDidActivate(httpSessionEvent);
            }
        }
    }
    
    protected void onSerialization() {
        for (Iterator iter = attributes.values().iterator(); iter.hasNext();) {
            Object value = iter.next();
            if (value instanceof HttpSessionActivationListener) {
                HttpSessionActivationListener listener = (HttpSessionActivationListener) value;
                listener.sessionWillPassivate(httpSessionEvent);
            }
        }
    }
    
    protected void onAddSate(Object key, Object oldValue, Object newValue) {
        String name = (String) key;
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
    
    protected void onRemoveState(Object key, Object oldValue) {
        String name = (String) key;
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
