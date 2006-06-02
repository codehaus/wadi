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
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.Evictable;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Lifecycle;
import org.codehaus.wadi.Manager;
import org.codehaus.wadi.ManagerConfig;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.PoolableInvocationWrapper;
import org.codehaus.wadi.PoolableInvocationWrapperPool;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.ValuePool;
import org.codehaus.wadi.web.AttributesFactory;
import org.codehaus.wadi.web.Router;
import org.codehaus.wadi.web.RouterConfig;
import org.codehaus.wadi.web.WADIHttpSession;
import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.WebSessionConfig;
import org.codehaus.wadi.web.WebSessionPool;
import org.codehaus.wadi.web.WebSessionWrapperFactory;
import org.codehaus.wadi.web.impl.DummyStatefulHttpServletRequestWrapperPool;
import org.codehaus.wadi.web.impl.Filter;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class StandardManager implements Lifecycle, WebSessionConfig, ContextualiserConfig, RouterConfig, Manager {

	protected final Log _log = LogFactory.getLog(getClass());

	protected final WebSessionPool _sessionPool;
	protected final AttributesFactory _attributesFactory;
	protected final ValuePool _valuePool;
	protected final WebSessionWrapperFactory _sessionWrapperFactory;
	protected final SessionIdFactory _sessionIdFactory;
	protected final Contextualiser _contextualiser;
	protected final Map _map;
	protected final Timer _timer;
	protected final Router _router;
	protected final boolean _errorIfSessionNotAcquired;
	protected final SynchronizedBoolean _acceptingSessions=new SynchronizedBoolean(true);
	protected final long _birthTime=System.currentTimeMillis();

	protected HttpSessionListener[] _sessionListeners;
	protected HttpSessionAttributeListener[] _attributeListeners;

	public StandardManager(WebSessionPool sessionPool, AttributesFactory attributesFactory, ValuePool valuePool, WebSessionWrapperFactory sessionWrapperFactory, SessionIdFactory sessionIdFactory, Contextualiser contextualiser, Map map, Router router, boolean errorIfSessionNotAcquired) {
		_sessionPool=sessionPool;
		_attributesFactory=attributesFactory;
		_valuePool=valuePool;
		_sessionWrapperFactory=sessionWrapperFactory;
		_sessionIdFactory=sessionIdFactory;
		_contextualiser=contextualiser;
		_map=map; // TODO - can we get this from Contextualiser
		_timer=new Timer();
		_router=router;
		_errorIfSessionNotAcquired=errorIfSessionNotAcquired;
	}

	protected ManagerConfig _config;

	public void init(ManagerConfig config) {
		if (_sessionListeners==null)
			_sessionListeners=new HttpSessionListener[]{};
		if (_attributeListeners==null)
			_attributeListeners=new HttpSessionAttributeListener[]{};

		_config=config;
		_sessionPool.init(this);
		_contextualiser.init(this);
		_router.init(this);
	}

	public void start() throws Exception {
		_log.info("starting");

		_contextualiser.promoteToExclusive(null);
		_contextualiser.start();
		ServletContext context=getServletContext();
		if (context==null) {
			_log.warn("null ServletContext");
		} else {
			context.setAttribute(Manager.class.getName(), this); // TODO - security risk ?
		}

		String version=getClass().getPackage().getImplementationVersion(); // maven2 puts version into MANIFEST.MF
		version=(version==null?System.getProperty("wadi.version"):version); // using Eclipse, I add it as a property
		_log.info("WADI-"+version+" successfully installed");
	}

	public void aboutToStop() throws Exception {
		// do nothing
	}

	public void stop() throws Exception {
		_acceptingSessions.set(false);
		// if we are clustered, partitions must be evacuated before sessions - hack
		aboutToStop();
		_contextualiser.stop();
		_log.info("stopped"); // although this sometimes does not appear, it IS called...
	}

	protected void notifySessionCreation(WebSession session) {
		WADIHttpSession httpSession = ensureTypeAndCast(session);
		int l=_sessionListeners.length;
		HttpSessionEvent hse=httpSession.getHttpSessionEvent();
		for (int i=0; i<l; i++)
			_sessionListeners[i].sessionCreated(hse);
	}

	protected void notifySessionDestruction(WebSession session) {
		WADIHttpSession httpSession = ensureTypeAndCast(session);
		int l=_sessionListeners.length;
		HttpSessionEvent hse=httpSession.getHttpSessionEvent();
		for (int i=0; i<l; i++)
			_sessionListeners[i].sessionDestroyed(hse); // actually - about-to-be-destroyed - hasn't happened yet - see SRV.15.1.14.1
	}

	private WADIHttpSession ensureTypeAndCast(WebSession session) {
		if (false == session instanceof WADIHttpSession) {
			throw new IllegalArgumentException(WADIHttpSession.class +
			" instance is expected.");
		}
		WADIHttpSession httpSession = (WADIHttpSession) session;
		return httpSession;
	}

	public void destroy() {
		_router.destroy();
		_contextualiser.destroy();
		_sessionPool.destroy();
	}

	protected boolean validateSessionName(String name) {
		return true;
	}

	public WebSession create() {
		String name=null;
		do {
			name=_sessionIdFactory.create(); // TODO - API on this class is wrong...
		} while (!validateSessionName(name));

		WebSession session=_sessionPool.take();
		long time=System.currentTimeMillis();
		session.init(time, time, _maxInactiveInterval, name);
		_map.put(name, session);
		notifySessionInsertion(name);
		notifySessionCreation(session);
		// TODO - somehow notify Evicter
		// _contextualiser.getEvicter().insert(session);
		if (_log.isDebugEnabled()) _log.debug("creation: "+name);
		return session;
	}

	public void destroy(WebSession session) {
		for (Iterator i=new ArrayList(session.getAttributeNameSet()).iterator(); i.hasNext();) // ALLOC ?
			session.removeAttribute((String)i.next()); // TODO - very inefficient
		// _contextualiser.getEvicter().remove(session);
		// TODO - somehow notify Evicter
		String name=session.getName();
		notifySessionDeletion(name);
		notifySessionDestruction(session);
		_map.remove(name);
		try {
			session.destroy();
		} catch (Exception e) {
			_log.warn("unexpected problem destroying session", e);
		}
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

	public WebSessionWrapperFactory getSessionWrapperFactory() {return _sessionWrapperFactory;}

	public SessionIdFactory getSessionIdFactory() {return _sessionIdFactory;}

	protected int _maxInactiveInterval=30*60; // 30 mins
	public int getMaxInactiveInterval(){return _maxInactiveInterval;}
	public void setMaxInactiveInterval(int interval){_maxInactiveInterval=interval;}

	// integrate with Filter instance
	protected Filter _filter;

	public void setFilter(Filter filter) {
		_filter=filter;
		_config.callback(this);
	}

	public boolean getDistributable(){return false;}

	public Contextualiser getContextualiser() {return _contextualiser;}

	public void setLastAccessedTime(Evictable evictable, long oldTime, long newTime) {_contextualiser.setLastAccessedTime(evictable, oldTime, newTime);}
	public void setMaxInactiveInterval(Evictable evictable, int oldInterval, int newInterval) {_contextualiser.setMaxInactiveInterval(evictable, oldInterval, newInterval);}

	public void expire(Motable motable) {
		destroy((WebSession)motable);
	}

	public Immoter getEvictionImmoter() {
		return ((AbstractExclusiveContextualiser)_contextualiser).getImmoter();
	} // HACK - FIXME

	public Timer getTimer() {return _timer;}

	public WebSessionPool getSessionPool() {return _sessionPool;}

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

	public long getBirthTime() {
		return _birthTime;
	}

	public long getUpTime() {
		return System.currentTimeMillis()-_birthTime;
	}

	// Deal with incoming/outgoing invocations... - taking over flow control
    
    public void contextualise(Invocation invocation) throws InvocationException {
        String key=invocation.getSessionKey();
        if (_log.isTraceEnabled()) _log.trace("potentially stateful request: "+key);
        
        if (key==null) {
            processStateless(invocation);
        } else {
            // already associated with a session...
            String name=_router.strip(key); // strip off any routing info...
            if (!_contextualiser.contextualise(invocation, name, null, null, false)) {
                if (_log.isErrorEnabled()) _log.error("could not acquire session: " + name);
                if (_errorIfSessionNotAcquired) // send the client a 503...
                    invocation.sendError(503, "session "+name+" is not known");
                else // process request without session - it may create a new one...
                    processStateless(invocation);
            }
        }
    }

    protected PoolableInvocationWrapperPool _pool=new DummyStatefulHttpServletRequestWrapperPool(); // TODO - init from _manager

    public void processStateless(Invocation invocation) throws InvocationException {
        // are we accepting sessions ? - otherwise relocate to another node...
        // sync point - expensive, but will only hit sessionless requests...
        if (!_acceptingSessions.get()) {
            // think about what to do here... relocate invoacation or error page ?
            _log.warn("sessionless request has arived during shutdown - permitting");
            // TODO
        }

        // no session yet - but may initiate one...
        PoolableInvocationWrapper wrapper=_pool.take();
        wrapper.init(invocation, null);
        invocation.invoke(wrapper);
        wrapper.destroy();
        _pool.put(wrapper);
    }

}
