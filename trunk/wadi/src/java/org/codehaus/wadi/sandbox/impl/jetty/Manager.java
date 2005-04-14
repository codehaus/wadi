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
package org.codehaus.wadi.sandbox.impl.jetty;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.codehaus.wadi.IdGenerator;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.AttributesPool;
import org.codehaus.wadi.sandbox.SessionPool;
import org.codehaus.wadi.sandbox.ValuePool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.SessionManager;

public class Manager extends org.codehaus.wadi.sandbox.impl.DistributableManager implements SessionManager {

    protected final Log _log = LogFactory.getLog(getClass());
    
    public Manager(SessionPool sessionPool, AttributesPool attributesPool, ValuePool valuePool, SessionWrapperFactory sessionWrapperFactory, IdGenerator sessionIdFactory, StreamingStrategy streamer) {
        super(sessionPool, attributesPool, valuePool, sessionWrapperFactory, sessionIdFactory, streamer);
        
        // we should install our filter and inject refs into it
        // we should probably manage the Contextualiser stack...
    }

    protected ServletHandler _handler;
    public void initialize(ServletHandler handler) {_handler=handler;}

    public ServletContext getServletContext() {return _handler.getServletContext();}

    public HttpSession getHttpSession(String id) {
        throw new UnsupportedOperationException();
    }

    public HttpSession newHttpSession(HttpServletRequest request) {
        throw new UnsupportedOperationException();
    }

    public void start() throws Exception {
        // pull out Manager
        // reference it...
        // pass the ServletContext to it somehow....
        // set up other stuff...
        getServletContext().setAttribute(Manager.class.getName(), this); // TODO - is putting ourselves in an attribute a security risk ?
        //super.start();
    }

    public void stop() throws InterruptedException {
        // shut everything down...
        // super.stop();
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
    
    protected boolean _httpOnly=true;
    public boolean getHttpOnly() {return _httpOnly;}
    public void setHttpOnly(boolean httpOnly) {_httpOnly=httpOnly;}
    
    protected boolean _secureCookies=false;
    public boolean getSecureCookies() {return _secureCookies;}
    public void setSecureCookies(boolean secureCookies) {_secureCookies=secureCookies;}
    
    protected boolean _useRequestedId=false;
    public boolean getUseRequestedId() {return _useRequestedId;}
    public void setUseRequestedId(boolean useRequestedId) {_useRequestedId=useRequestedId;}

}
