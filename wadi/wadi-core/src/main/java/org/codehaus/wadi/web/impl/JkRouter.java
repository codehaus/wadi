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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.impl.WADIRuntimeException;
import org.codehaus.wadi.web.Router;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1737 $
 */
public class JkRouter implements Router {
    private static final Log _log = LogFactory.getLog(JkRouter.class);
    private static final String SESSION_COOKIE_NAME = "JSESSIONID";
    
    private final String suffix;

    public JkRouter(String info) {
        suffix = "." + info;
    }

    public String strip(String session) {
        int i = session.lastIndexOf(".");
        if (i < 0) {
            return session;
        } else {
            return session.substring(0, i);
        }
    }

    public String augment(String id) {
        return augment(id, suffix);
    }

    public boolean reroute(Invocation invocation) {
        WebInvocation context = (WebInvocation) invocation;
        HttpServletRequest req = context.getHreq();
        HttpServletResponse res = context.getHres();
        String id = req.getRequestedSessionId();
        if (id.endsWith(suffix)) {
            return false;
        }
        if (req.isRequestedSessionIdFromCookie()) {
            return rerouteCookie(req, res, id, suffix);
        } else {
            throw new WADIRuntimeException("Can only reroute cookies");
        }
    }

    protected boolean rerouteCookie(HttpServletRequest req, HttpServletResponse res, String id, String target) {
        String oldId = id;
        String newId = augment(id, suffix);

        if (_log.isInfoEnabled()) {
            _log.info("rerouting cookie: " + oldId + " -> " + newId);
        }

        Cookie[] cookies = req.getCookies();
        for (int i = 0; i < cookies.length; i++) {
            Cookie cookie = cookies[i];
            if (cookie.getName().equalsIgnoreCase(SESSION_COOKIE_NAME) && cookie.getValue().equals(oldId)) {
                // name, path and domain must match those on client side, for cookie to be updated in browser...
                String cookiePath = getSessionCookiePath(req);
                if (cookiePath != null) {
                    cookie.setPath(cookiePath);
                }
                String cookieDomain = getSessionCookieDomain();
                if (cookieDomain != null) {
                    cookie.setDomain(cookieDomain);
                }
                // the session id with redirected routing info
                cookie.setValue(newId); 
                res.addCookie(cookie);
            }
        }
        return false;
    }

    protected String augment(String id, String target) {
        int i = id.lastIndexOf(".");
        if (i < 0) {
            return id + target;
        } else {
            if (id.endsWith(target)) {
                // it's our routing info - leave it
                return id;
            } else {
                // it's someone else's - replace it
                return id.substring(0, i) + target; 
            }
        }
    }

    protected String getSessionCookiePath(HttpServletRequest req) {
        return req.getContextPath();
    }

    protected String getSessionCookieDomain() {
        return null;
    }

}
