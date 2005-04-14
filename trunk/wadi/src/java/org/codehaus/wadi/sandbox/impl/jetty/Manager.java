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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

//import org.codehaus.wadi.impl.TomcatIdGenerator;
//import org.codehaus.wadi.sandbox.AttributesPool;
//import org.codehaus.wadi.sandbox.SessionPool;
//import org.codehaus.wadi.sandbox.ValuePool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.SessionManager;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.InputStreamResource;

public class Manager implements SessionManager {

    protected final Log _log = LogFactory.getLog(getClass());
    protected org.codehaus.wadi.sandbox.impl.Manager _wadi;
    
//    public Manager(SessionPool sessionPool, AttributesPool attributesPool, ValuePool valuePool) {
//        super(sessionPool, attributesPool, valuePool, new SessionWrapperFactory(), new TomcatIdGenerator());
        
        // we should install our filter and inject refs into it
        // we should probably manage the Contextualiser stack...
    //}

    protected ServletHandler _handler;
    public void initialize(ServletHandler handler) {_handler=handler;}

    public ServletContext getServletContext() {return _handler.getServletContext();}

    public HttpSession getHttpSession(String id) {
        throw new UnsupportedOperationException();
    }

    public HttpSession newHttpSession(HttpServletRequest request) {
        throw new UnsupportedOperationException();
    }

    public boolean isStarted(){return _wadi.isStarted();}
    
    
    public void start() throws Exception {
        // load Spring config
        String localConfig=System.getProperty("wadi.config");
        String config=localConfig==null?_defaultConfig:localConfig;
        loadSpringConfig(config);
        
        // pull out Manager
        // reference it...
        // pass the ServletContext to it somehow....
        // set up other stuff...
        getServletContext().setAttribute(Manager.class.getName(), this); // TODO - is putting ourselves in an attribute a security risk ?
    }

    public void stop() throws InterruptedException {
        // shut everything down...
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
    
    protected int _maxInactiveInterval=30*60;
    public int getMaxInactiveInterval(){return _maxInactiveInterval;}
    public void setMaxInactiveInterval(int interval){_maxInactiveInterval=interval;}
    
    List _eventListeners=new ArrayList();
    public void addEventListener(EventListener el){_eventListeners.add(el);}
    public void removeEventListener(EventListener el){_eventListeners.remove(el);}
    
    
    protected final String _defaultConfig="/WEB-INF/wadi2-web.xml"; // TODO - change name back later..
    
    public void loadSpringConfig(String config)   {
        _log.debug("starting");
        _log.info("WADI-2.0beta : Web Application Distribution Infrastructure (http://wadi.codehaus.org)");
        
        InputStream is=getServletContext().getResourceAsStream(config);
        
        if (is!=null)
        {
            DefaultListableBeanFactory dlbf=new DefaultListableBeanFactory();
            PropertyPlaceholderConfigurer cfg=new PropertyPlaceholderConfigurer();
            dlbf.registerSingleton("Manager", this);
            new XmlBeanDefinitionReader(dlbf).loadBeanDefinitions(new InputStreamResource(is));
            cfg.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK);
            cfg.postProcessBeanFactory(dlbf);
            
            _wadi=(org.codehaus.wadi.sandbox.impl.Manager)dlbf.getBean("SessionManager");
            
            _log.info("configured from WADI descriptor: "+config);
        }
        else
            _log.warn("could not find WADI descriptor: "+config);
    }
    
}
