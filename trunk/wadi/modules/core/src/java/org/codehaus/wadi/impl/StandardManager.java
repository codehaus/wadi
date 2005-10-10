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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.AttributesFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.Evictable;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Lifecycle;
import org.codehaus.wadi.ManagerConfig;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.Router;
import org.codehaus.wadi.RouterConfig;
import org.codehaus.wadi.Session;
import org.codehaus.wadi.SessionConfig;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionPool;
import org.codehaus.wadi.SessionWrapperFactory;
import org.codehaus.wadi.ValuePool;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class StandardManager implements Lifecycle, SessionConfig, ContextualiserConfig, RouterConfig {

    protected final Log _log = LogFactory.getLog(getClass());

    protected final SessionPool _sessionPool;
    protected final AttributesFactory _attributesFactory;
    protected final ValuePool _valuePool;
    protected final SessionWrapperFactory _sessionWrapperFactory;
    protected final SessionIdFactory _sessionIdFactory;
    protected final Contextualiser _contextualiser;
    protected final Map _map;
    protected final Timer _timer;
    protected final Router _router;
    protected final boolean _errorIfSessionNotAcquired=true; // TODO - parameterise
    protected final SynchronizedBoolean _acceptingSessions=new SynchronizedBoolean(true);
    
    protected HttpSessionListener[] _sessionListeners;
    protected HttpSessionAttributeListener[] _attributeListeners;
    
    public StandardManager(SessionPool sessionPool, AttributesFactory attributesFactory, ValuePool valuePool, SessionWrapperFactory sessionWrapperFactory, SessionIdFactory sessionIdFactory, Contextualiser contextualiser, Map map, Router router) {
        _sessionPool=sessionPool;
        _attributesFactory=attributesFactory;
        _valuePool=valuePool;
        _sessionWrapperFactory=sessionWrapperFactory;
        _sessionIdFactory=sessionIdFactory;
        _contextualiser=contextualiser;
        _map=map; // TODO - can we get this from Contextualiser
        _timer=new Timer();
        _router=router;
    }

    protected ManagerConfig _config;
    
    public void init(ManagerConfig config) {
    	_config=config;
        _sessionPool.init(this);
        _contextualiser.init(this);
        _router.init(this);
    }

    protected boolean _started;

    public boolean isStarted(){return _started;}
    
    public void start() throws Exception {
        _log.info("starting");
        _contextualiser.promoteToExclusive(null);
        _contextualiser.start();
        ServletContext context=getServletContext();
        if (context==null)
        	_log.warn("null ServletContext");
        else
        	context.setAttribute(StandardManager.class.getName(), this); // TODO - security risk ?
        _started=true;
    }

    public void stop() throws Exception {
        _started=false;
        _acceptingSessions.set(false);
        _contextualiser.stop();
        _log.info("stopped"); // although this sometimes does not appear, it IS called...
    }

    public void destroy() {
        _router.destroy();
        _contextualiser.destroy();
        _sessionPool.destroy();
    }
    
    public Session create() {
        Session session=_sessionPool.take();
        long time=System.currentTimeMillis();
        String name=_sessionIdFactory.create(); // TODO - API on this class is wrong...
        session.init(time, time, _maxInactiveInterval, name);
        _map.put(name, session);
        notifySessionInsertion(name);
        // _contextualiser.getEvicter().insert(session);
        if (_log.isDebugEnabled()) _log.debug("creation: "+name);
        return session;
    }

    public void destroy(Session session) {
        for (Iterator i=new ArrayList(session.getAttributeNameSet()).iterator(); i.hasNext();) // ALLOC ?
            session.removeAttribute((String)i.next()); // TODO - very inefficient
        // _contextualiser.getEvicter().remove(session);
        String name=session.getName();
        notifySessionDeletion(name);
        _map.remove(name);
        session.destroy();
        _sessionPool.put(session);
        if (_log.isDebugEnabled()) _log.debug("destruction: "+name);
    }

    //----------------------------------------
    // Listeners

    public HttpSessionListener[] getSessionListeners() {
    	return _sessionListeners;
    }
    
    public void setSessionListeners(HttpSessionListener[] sessionListeners) {
    	_sessionListeners=sessionListeners;
    }
    
    public HttpSessionAttributeListener[] getAttributeListeners() {
    	return _attributeListeners;
    }
    
    public void setAttributelisteners(HttpSessionAttributeListener[] attributeListeners) {
    	_attributeListeners=attributeListeners;
    }
    
    // Context stuff
    public ServletContext getServletContext() {return _config.getServletContext();}

    public AttributesFactory getAttributesFactory() {return _attributesFactory;}
    public ValuePool getValuePool() {return _valuePool;}

    public StandardManager getManager(){return this;}

    // this should really be abstract, but is useful for testing - TODO

    public SessionWrapperFactory getSessionWrapperFactory() {return _sessionWrapperFactory;}

    public SessionIdFactory getSessionIdFactory() {return _sessionIdFactory;}

    protected int _maxInactiveInterval=30*60; // 30 mins
    public int getMaxInactiveInterval(){return _maxInactiveInterval;}
    public void setMaxInactiveInterval(int interval){_maxInactiveInterval=interval;}

    // integrate with Filter instance
    protected Filter _filter;

    public void setFilter(Filter filter) {
    	_filter=filter;
    	_config.callback(this);
    	
    	if (_sessionListeners==null)
    		_sessionListeners=new HttpSessionListener[]{};
    	if (_attributeListeners==null)
    		_attributeListeners=new HttpSessionAttributeListener[]{};
    }

    public boolean getDistributable(){return false;}

    public Contextualiser getContextualiser() {return _contextualiser;}

    public void setLastAccessedTime(Evictable evictable, long oldTime, long newTime) {_contextualiser.setLastAccessedTime(evictable, oldTime, newTime);}
    public void setMaxInactiveInterval(Evictable evictable, int oldInterval, int newInterval) {_contextualiser.setMaxInactiveInterval(evictable, oldInterval, newInterval);}

    public void expire(Motable motable) {
        destroy((Session)motable);
    }

    public Immoter getEvictionImmoter() {
        return ((AbstractExclusiveContextualiser)_contextualiser).getImmoter();
        } // HACK - FIXME

    public Timer getTimer() {return _timer;}

    public SessionPool getSessionPool() {return _sessionPool;}
    
    public Router getRouter() {return _router;}

    // Container integration... - override if a particular Container can do better... 
    
    public int getHttpPort(){return Integer.parseInt(System.getProperty("http.port"));} // TODO - temporary hack...

    public String getSessionCookieName()  {return "JSESSIONID";}

    public String getSessionCookiePath(HttpServletRequest req){return req.getContextPath();}

    public String getSessionCookieDomain(){return null;}

    public String getSessionUrlParamName(){return "jsessionid";}

    public boolean getErrorIfSessionNotAcquired() {return _errorIfSessionNotAcquired;}
    
    protected SynchronizedInt _errorCounter=new SynchronizedInt(0);
    
    public void incrementErrorCounter() {_errorCounter.increment();}
    
    public int getErrorCount() {return _errorCounter.get();}
    
    public SynchronizedBoolean getAcceptingSessions() {return _acceptingSessions;}
    
    public void notifySessionInsertion(String name) {
    }
    
    public void notifySessionDeletion(String name) {
    }
    
    // called on new host...
    public void notifySessionRelocation(String name) {
    }

}
