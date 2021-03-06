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

import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.web.WebSession;

/**
 * Wraps a Session instance, presenting ONLY an HttpSession facade to the application.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1893 $
 */

public class WebSessionWrapper implements HttpSession {
    private static final Log log = LogFactory.getLog(WebSessionWrapper.class);
    
    protected final WebSession _session;

    public WebSessionWrapper(WebSession session) {
        _session = session;
    }

    public long getCreationTime() {
        return _session.getCreationTime();
    }

    public long getLastAccessedTime() {
        return _session.getLastAccessedTime();
    }

    public void setMaxInactiveInterval(int interval) {
        _session.setMaxInactiveInterval(interval);
    }

    public int getMaxInactiveInterval() {
        return _session.getMaxInactiveInterval();
    }

    public boolean isNew() {
        return _session.isNew();
    }

    public String getId() {
        return _session.getId();
    }

    public void setAttribute(String name, Object value) {
        if (null == value) {
            _session.removeAttribute(name);
        } else {
            _session.setAttribute(name, value);
        }
    }

    public Object getAttribute(String name) {
        return _session.getAttribute(name);
    }

    public void removeAttribute(String name) {
        _session.removeAttribute(name);
    }

    public Enumeration getAttributeNames() {
        return _session.getAttributeNameEnumeration();
    }

    public String[] getValueNames() {
        return _session.getAttributeNameStringArray();
    }

    public Object getValue(String name) {
        return getAttribute(name);
    }

    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    public void removeValue(String name) {
        removeAttribute(name);
    }

    public ServletContext getServletContext() {
        return _session.getConfig().getServletContext();
    }

    public void invalidate() {
        try {
            _session.destroy();
        } catch (Exception e) {
            log.error(e);
        }
    }

    protected static final HttpSessionContext _httpSessionContext = new HttpSessionContext() {
        protected final Enumeration _emptyEnumeration = Collections.enumeration(Collections.EMPTY_LIST);

        public HttpSession getSession(String sessionId) {
            return null;
        }

        public Enumeration getIds() {
            return _emptyEnumeration;
        }
    };

    public HttpSessionContext getSessionContext() {
        return _httpSessionContext;
    }

}
