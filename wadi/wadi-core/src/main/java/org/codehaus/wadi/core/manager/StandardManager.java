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
package org.codehaus.wadi.core.manager;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.core.WADIRuntimeException;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.contextualiser.InvocationContext;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.core.session.SessionFactory;

/**
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class StandardManager implements Lifecycle, Manager {
	private static final Log log = LogFactory.getLog(StandardManager.class);

    private final SessionFactory sessionFactory;
    private final SessionIdFactory sessionIdFactory;
    private final Contextualiser contextualiser;
    private final ConcurrentMotableMap motableMap;
    private final Router router;
    private final AtomicBoolean acceptingSessions = new AtomicBoolean(true);
    private final SessionMonitor sessionMonitor;
    private ManagerConfig config;
    private int maxInactiveInterval = 30 * 60;

    public StandardManager(SessionFactory sessionFactory,
            SessionIdFactory sessionIdFactory,
            Contextualiser contextualiser,
            ConcurrentMotableMap motableMap,
            Router router,
            SessionMonitor sessionMonitor) {
        if (null == sessionFactory) {
            throw new IllegalArgumentException("sessionFactory is required");
        } else if (null == sessionIdFactory) {
            throw new IllegalArgumentException("sessionIdFactory is required");
        } else if (null == contextualiser) {
            throw new IllegalArgumentException("contextualiser is required");
        } else if (null == motableMap) {
            throw new IllegalArgumentException("motableMap is required");
        } else if (null == router) {
            throw new IllegalArgumentException("router is required");
        } else if (null == sessionMonitor) {
            throw new IllegalArgumentException("sessionMonitor is required");
        }
        this.sessionFactory = sessionFactory;
        this.sessionIdFactory = sessionIdFactory;
        this.contextualiser = contextualiser;
        this.motableMap = motableMap;
        this.router = router;
        this.sessionMonitor = sessionMonitor;
        
        sessionFactory.setManager(this);
    }

    public void init(ManagerConfig config) {
        this.config = config;
    }

    public void start() throws Exception {
        contextualiser.promoteToExclusive(null);
        contextualiser.start();
        acceptingSessions.set(true);
        log.info("WADI Manager Successfully Started");
    }

    public void stop() throws Exception {
        acceptingSessions.set(false);
        contextualiser.stop();
    }

    public Session createWithName(String name) throws SessionAlreadyExistException {
        if (!validateSessionName(name)) {
            throw new SessionAlreadyExistException(name);
        }
        return createSession(name);
    }

    public Session create(Invocation invocation) {
        String name = null;
        do {
            name = sessionIdFactory.create();
        } while (!validateSessionName(name));
        return createSession(name);
    }

    public void destroy(Session session) {
        if (log.isDebugEnabled()) {
            log.debug("Destroy [" + session + "]");
        }
        motableMap.remove(session.getName());
        sessionMonitor.notifySessionDestruction(session);
        onSessionDestruction(session);
    }

    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public void setMaxInactiveInterval(int interval) {
        maxInactiveInterval = interval;
    }
    
    public SessionIdFactory getSessionIdFactory() {
        return sessionIdFactory;
    }

    // integrate with Filter instance
    public void triggerCallback() {
        config.callback(this, sessionMonitor, sessionFactory);
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
        String name = router.strip(key);
        boolean contextualised = contextualiser.contextualise(invocation, name, null, false);
        if (!contextualised) {
            log.debug("Could not acquire session [" + name + "]");
            if (invocation.isErrorIfSessionNotAcquired()) {
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
        if (!acceptingSessions.get()) {
            // think about what to do here... relocate invoacation or error page ?
            throw new WADIRuntimeException("sessionless request has arrived during shutdown");
        }

        // no session yet - but may initiate one...
        InvocationContext context = invocation.newContext(null);
        invocation.invoke(context);
        return true;
    }

    protected boolean validateSessionName(String name) {
        return true;
    }

    protected Session createSession(String name) {
        Session session = sessionFactory.create();
        long time = System.currentTimeMillis();
        session.init(time, time, maxInactiveInterval, name);
        motableMap.put(name, session);
        sessionMonitor.notifySessionCreation(session);
        onSessionCreation(session);
        if (log.isDebugEnabled()) {
            log.debug("creation: " + name);
        }
        return session;
    }
    
    protected void onSessionCreation(Session session) {
    }

    protected void onSessionDestruction(Session session) {
    }
    
}
