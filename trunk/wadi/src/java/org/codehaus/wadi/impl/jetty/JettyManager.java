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
package org.codehaus.wadi.impl.jetty;

import java.net.InetSocketAddress;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.HttpProxy;
import org.codehaus.wadi.Router;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.SessionWrapperFactory;
import org.codehaus.wadi.Streamer;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.impl.StandardManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.SessionManager;

public class JettyManager extends org.codehaus.wadi.impl.DistributableManager implements SessionManager {

    protected final Log _log = LogFactory.getLog(getClass());

    public JettyManager(SessionPool sessionPool, AttributesFactory attributesFactory, ValuePool valuePool, SessionWrapperFactory sessionWrapperFactory, SessionIdFactory sessionIdFactory, Contextualiser contextualiser, Map sessionMap, Router router, Streamer streamer, boolean accessOnLoad, String clusterUri, String clusterName, String nodeName, HttpProxy httpProxy, InetSocketAddress httpAddress, int numBuckets) {
        super(sessionPool, attributesFactory, valuePool, sessionWrapperFactory, sessionIdFactory, contextualiser, sessionMap, router, streamer, accessOnLoad, clusterUri, clusterName, nodeName, httpProxy, httpAddress, numBuckets);
    }

    // DistributableManager - WADI
    
    public ServletContext getServletContext() {return _handler.getServletContext();}

    // Lifecyle - Jetty & WADI
    
    public void start() throws Exception {
        getServletContext().setAttribute(StandardManager.class.getName(), this); // TODO - is putting ourselves in an attribute a security risk ?
        super.start();
    }

    public void stop() {
        try {
            super.stop();
        } catch (Exception e) {
      _log.warn("unexpected problem shutting down", e);
        }
    }

    // SessionManager - Jetty
    
    protected ServletHandler _handler;
    public void initialize(ServletHandler handler) {_handler=handler; init();}

    protected boolean _httpOnly=true;
    public boolean getHttpOnly() {return _httpOnly;}
    public void setHttpOnly(boolean httpOnly) {_httpOnly=httpOnly;}

    protected boolean _secureCookies=false;
    public boolean getSecureCookies() {return _secureCookies;}
    public void setSecureCookies(boolean secureCookies) {_secureCookies=secureCookies;}

    protected boolean _useRequestedId=false;
    public boolean getUseRequestedId() {return _useRequestedId;}
    public void setUseRequestedId(boolean useRequestedId) {_useRequestedId=useRequestedId;}

    public HttpSession newHttpSession(HttpServletRequest request) {
        return create().getWrapper();
    }

    public HttpSession getHttpSession(String id) {
        //throw new UnsupportedOperationException();
        return null; // FIXME - this will be the container trying to 'refresh' a session...
    }

    // cut-n-pasted from Jetty src - aarg !
    // Greg uses Apache-2.0 as well - so no licensing issue as yet - TODO
    
    public Cookie
    getSessionCookie(javax.servlet.http.HttpSession session,boolean requestIsSecure)
    {
        if (_handler.isUsingCookies())
        {
            javax.servlet.http.Cookie cookie=getHttpOnly()
            ?new org.mortbay.http.HttpOnlyCookie(SessionManager.__SessionCookie,session.getId())
                    :new javax.servlet.http.Cookie(SessionManager.__SessionCookie,session.getId());
            String domain=_handler.getServletContext().getInitParameter(SessionManager.__SessionDomain);
            String maxAge=_handler.getServletContext().getInitParameter(SessionManager.__MaxAge);
            String path=_handler.getServletContext().getInitParameter(SessionManager.__SessionPath);
            if (path==null)
                path=getUseRequestedId()?"/":_handler.getHttpContext().getContextPath();
            if (path==null || path.length()==0)
                path="/";
    
            if (domain!=null)
                cookie.setDomain(domain);
            if (maxAge!=null)
                cookie.setMaxAge(Integer.parseInt(maxAge));
            else
                cookie.setMaxAge(-1);
    
            cookie.setSecure(requestIsSecure && getSecureCookies());
            cookie.setPath(path);
    
            return cookie;
        }
        return null;
    }

}
