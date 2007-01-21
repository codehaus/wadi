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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Lifecycle;
import org.codehaus.wadi.Manager;
import org.codehaus.wadi.ManagerConfig;
import org.codehaus.wadi.PoolableInvocationWrapper;
import org.codehaus.wadi.PoolableInvocationWrapperPool;
import org.codehaus.wadi.SessionAlreadyExistException;
import org.codehaus.wadi.SessionIdFactory;
import org.codehaus.wadi.SessionMonitor;
import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.web.Router;
import org.codehaus.wadi.web.WebSession;
import org.codehaus.wadi.web.WebSessionFactory;
import org.codehaus.wadi.web.impl.DummyStatefulHttpServletRequestWrapperPool;
import org.codehaus.wadi.web.impl.Filter;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

/**
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class StandardManager implements Lifecycle, Manager {
	private static final Log log = LogFactory.getLog(StandardManager.class);

    private final WebSessionFactory sessionFactory;
    protected final SessionIdFactory _sessionIdFactory;
    protected final Contextualiser _contextualiser;
    protected final ConcurrentMotableMap _map;
    protected final Router _router;
    protected final boolean _errorIfSessionNotAcquired;
    protected final SynchronizedBoolean _acceptingSessions = new SynchronizedBoolean(true);
    private final SessionMonitor sessionMonitor;
    protected PoolableInvocationWrapperPool _pool = new DummyStatefulHttpServletRequestWrapperPool();
    protected ManagerConfig _config;
    protected int _maxInactiveInterval = 30 * 60;
    protected Filter _filter;

    public StandardManager(WebSessionFactory sessionFactory,
            SessionIdFactory sessionIdFactory,
            Contextualiser contextualiser,
            ConcurrentMotableMap map,
            Router router,
            SessionMonitor sessionMonitor,
            boolean errorIfSessionNotAcquired) {
        if (null == sessionFactory) {
            throw new IllegalArgumentException("sessionFactory is required");
        } else if (null == sessionIdFactory) {
            throw new IllegalArgumentException("sessionIdFactory is required");
        } else if (null == contextualiser) {
            throw new IllegalArgumentException("contextualiser is required");
        } else if (null == map) {
            throw new IllegalArgumentException("map is required");
        } else if (null == router) {
            throw new IllegalArgumentException("router is required");
        } else if (null == sessionMonitor) {
            throw new IllegalArgumentException("sessionMonitor is required");
        }
        this.sessionFactory = sessionFactory;
        _sessionIdFactory = sessionIdFactory;
        _contextualiser = contextualiser;
        _map = map;
        _router = router;
        _errorIfSessionNotAcquired = errorIfSessionNotAcquired;
        this.sessionMonitor = sessionMonitor;
        
        sessionFactory.getWebSessionConfig().setManager(this);
    }

    public void init(ManagerConfig config) {
        _config = config;
    }

    public void start() throws Exception {
        _contextualiser.promoteToExclusive(null);
        _contextualiser.start();
        String version = getClass().getPackage().getImplementationVersion();
        version = (version == null ? System.getProperty("wadi.version") : version);
        log.info("WADI-" + version + " successfully installed");
    }

    public void stop() throws Exception {
        _acceptingSessions.set(false);
        _contextualiser.stop();
    }

    public WebSession createWithName(String name) throws SessionAlreadyExistException {
        if (!validateSessionName(name)) {
            throw new SessionAlreadyExistException(name);
        }
        return createSession(name);
    }

    public WebSession create(Invocation invocation) {
        String name = null;
        do {
            name = _sessionIdFactory.create();
        } while (!validateSessionName(name));
        return createSession(name);
    }

    public void destroy(WebSession session) {
        if (log.isDebugEnabled()) {
            log.debug("Destroy [" + session + "]");
        }
        _map.remove(session.getName());
        sessionMonitor.notifySessionDestruction(session);
        onSessionDestruction(session);
    }

    public int getMaxInactiveInterval() {
        return _maxInactiveInterval;
    }

    public void setMaxInactiveInterval(int interval) {
        _maxInactiveInterval = interval;
    }
    
    public SessionIdFactory getSessionIdFactory() {
        return _sessionIdFactory;
    }

    // integrate with Filter instance
    public void setFilter(Filter filter) {
        _filter = filter;
        _config.callback(this, sessionMonitor, sessionFactory.getWebSessionConfig());
    }

    public boolean contextualise(Invocation invocation) throws InvocationException {
        String key = invocation.getSessionKey();
        if (null == key) {
            return processStateless(invocation);
        } else {
            return processStateful(invocation);
        }
    }

    protected boolean processStateful(Invocation invocation) throws InvocationException {
        String key = invocation.getSessionKey();
        // already associated with a session. strip off any routing info.
        String name = _router.strip(key);
        boolean contextualised = _contextualiser.contextualise(invocation, name, null, false);
        if (!contextualised) {
            log.error("Could not acquire session [" + name + "]");
            if (_errorIfSessionNotAcquired) {
                invocation.sendError(503, "Session [" + name + "] is not known");
            } else {
                contextualised = processStateless(invocation);
            }
        }
        return contextualised;
    }

    protected boolean processStateless(Invocation invocation) throws InvocationException {
        // are we accepting sessions ? - otherwise relocate to another node...
        // sync point - expensive, but will only hit sessionless requests...
        if (!_acceptingSessions.get()) {
            // think about what to do here... relocate invoacation or error page ?
            throw new WADIRuntimeException("sessionless request has arrived during shutdown");
        }

        // no session yet - but may initiate one...
        PoolableInvocationWrapper wrapper = _pool.take();
        wrapper.init(invocation, null);
        invocation.invoke(wrapper);
        wrapper.destroy();
        _pool.put(wrapper);
        return true;
    }

    protected boolean validateSessionName(String name) {
        return true;
    }

    protected WebSession createSession(String name) {
        WebSession session = sessionFactory.create();
        long time = System.currentTimeMillis();
        session.init(time, time, _maxInactiveInterval, name);
        _map.put(name, session);
        sessionMonitor.notifySessionCreation(session);
        onSessionCreation(session);
        if (log.isDebugEnabled()) {
            log.debug("creation: " + name);
        }
        return session;
    }
    
    protected void onSessionCreation(WebSession session) {
    }

    protected void onSessionDestruction(WebSession session) {
    }
    
}
