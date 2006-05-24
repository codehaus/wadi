/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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
package org.codehaus.wadi.axis2;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;
import javax.servlet.ServletContext;
import org.apache.axis2.session.Session;
import org.apache.axis2.session.SessionIdFactory;
import org.apache.axis2.session.SessionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Manager;
import org.codehaus.wadi.ManagerConfig;
import org.codehaus.wadi.WADIHttpSession;
import org.codehaus.wadi.impl.AtomicallyReplicableSessionFactory;
import org.codehaus.wadi.impl.SpringManagerFactory;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.Rendezvous;

public class Axis2Manager implements SessionManager, ManagerConfig {
    
    protected final Log _log=LogFactory.getLog(getClass());
    protected final Axis2SessionWrapperFactory _wrapperFactory=new Axis2SessionWrapperFactory();
    protected final PooledExecutor _pool=new PooledExecutor();
    
    // integrated
    protected int _maxInactiveInterval; // integrated
    protected Manager _wadi;
    
    // still to integrate
    protected SessionIdFactory _idFactory; // can we integrate this somehow ? - wrap up in our own API and pass through to Spring via sysprop
    protected long _checkInterval; // needs integration with Evicter ? - pass through to Spring via sysprop
    protected int _maxActiveSessions; // needs integration with Evicter ? - pass through to Spring via sysprop
    
    
    public void start() throws Exception {
        _log.debug("starting...");
        try {
            // we should probably acquire this through some backptr from the container...
            String path=System.getProperty("axis2.repo")+"/server/wadi-axis2.xml";
            InputStream descriptor=new FileInputStream(path);
            _wadi=SpringManagerFactory.create(descriptor, "SessionManager", new AtomicallyReplicableSessionFactory(), new Axis2SessionWrapperFactory());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        _wadi.setMaxInactiveInterval(_maxInactiveInterval);
        _wadi.init(this);
        _wadi.start();
        _log.debug("...started");
    }
    
    public void stop() throws Exception {
        _log.debug("stopping...");
        _wadi.stop();   
        _log.debug("...stopped");
    }
    
    public Axis2Manager() throws Exception {
        start();
    }
    
    // WADI ManagerConfig
    
    public ServletContext getServletContext() {
        return null; // Not relevant to Axis2 - this should not be on the Manager interface
    }
    
    public void callback(Manager arg0) {
        _log.debug("callback()");
    }
    
    // Axis2 SessionManager
    
    // Session lifecycle
    
    public Session findSession(String key) {
        _log.debug("find("+key+")");
        Axis2Invocation invocation=Axis2Invocation.getThreadLocalInstance();
        assert(key.equals(invocation.getKey()));
        return invocation.getSession();
    }
    
    public Session createSession() {
        _log.debug("create()");
        Axis2Invocation invocation=Axis2Invocation.getThreadLocalInstance();
        org.codehaus.wadi.Session session=_wadi.create();
        invocation.setKey(session.getId());
        invocation.setSession((Axis2Session)session);
        WADIHttpSession httpSession = (WADIHttpSession)session;
        return (Axis2Session)httpSession.getWrapper();
    }
    
    // Rajith might want to consider moving his invalidate method onto the Session itself...
    public void release(Session session) {
        _log.debug("invalidate("+session.getId()+")");
        Axis2Invocation invocation=Axis2Invocation.getThreadLocalInstance();
        // assume that session must have beem retrieved at least once... 
        // and confirm that it is the correct one for this Thread/Invocation
        assert(session==invocation.getSession());
        invocation.setSession(null);
        session.invalidate();
    }
    
    // Enter/Leave (Activate/Passivate):
    // this is nasty - we need to hold up current thread whilst we descend to bottom of Contextualiser stack,
    // then when Invocation.invoke() is called, we need to run the current Thread through the container. As it
    // leaves the container, we need to hold it up again as we ascend the Contextualiser stack, then wake it up
    // and allow it to pass out of the Container - yikes ! And if anything in WADI is counting on Thread identity
    // being the same throughout the whole Invocation, we may hit trouble... - alternatively, we can refactor a lot of WADI...
    
    // called as an Axis2 invocation enters the container...
    
    public void activateSession(String key) {
        Axis2Invocation invocation=Axis2Invocation.getThreadLocalInstance();
        invocation.init(_wadi, key);
        _log.debug("enter("+key+")");
        
        // start Helper thread ...
        try {
            _pool.execute(invocation);
        } catch (InterruptedException e) {
            _log.error(e); // FIXME
        }
        // ... and wait for it to descend contextualiser stack...
        try {
            _log.trace(Thread.currentThread().getName()+": Invocation thread entering RV[1]");
            invocation.getRendezvous().rendezvous(null);
            _log.trace(Thread.currentThread().getName()+": Invocation thread leaving RV[1]");
        } catch (InterruptedException e) {
            _log.error(e);
        }
        // ...when Helper thread hits bottom of contextualiser stack,
        // we wake up and continue journey into container...
    }
    
    // called as an Axis2 invocation leaves the container...
    
    public void passivateSession(String key) {
        Axis2Invocation invocation=Axis2Invocation.getThreadLocalInstance();
        _log.debug("leave("+key+")");
        Rendezvous rv=invocation.getRendezvous();
        // Invocation thread has just traversed the container
        // rendezvous with Helper thread so that it may start to ascend contextualiser stack
        try {
            _log.trace(Thread.currentThread().getName()+": Invocation thread entering RV[2]");
            rv.rendezvous(null);
            _log.trace(Thread.currentThread().getName()+": Invocation thread leaving RV[2]");
        } catch (InterruptedException e) {
            _log.error(e);
        }
        // Rendezvous with it again at the top of the contextualiser stack...
        try {
            _log.trace(Thread.currentThread().getName()+": Invocation thread entering RV[3]");
            rv.rendezvous(null);
            _log.trace(Thread.currentThread().getName()+": Invocation thread leaving RV[3]");
        } catch (InterruptedException e) {
            _log.error(e);
        }
        
        // tidy up:
        invocation.clear();
        // then continue on out of the container...
    }
    
    // CheckInterval
    
    public long getCheckInterval() {
        return _checkInterval;
    }
    
    public void setCheckInterval(int checkInterval) {
        _checkInterval=checkInterval;
    }
    
    // DefaultMaxInactiveInterval
    
    public int getDefaultMaxInactiveInterval() {
        return _maxInactiveInterval;
    }
    
    public void setDefaultMaxInactiveInterval(int maxInactiveInterval) {
        _maxInactiveInterval=maxInactiveInterval;
    }
    
    // MaxActiveSessions
    
    public int getMaxActiveSessions() {
        return _maxActiveSessions;
    }
    
    public void setMaxActiveSessions(int maxActiveSessions) {
        _maxActiveSessions=maxActiveSessions;
    }
    
    // SessionIdFactory
    
    public SessionIdFactory getSessionIdFactory() {
        return _idFactory;
    }
    
    public void setSessionIdFactory(SessionIdFactory idFactory) {
        _idFactory=idFactory;
    }
    
    // Sessions
    
    /*
     * @deprecated
     */
    public Hashtable getSessions() {
        throw new UnsupportedOperationException("NYI");
    }
    
    /*
     * @deprecated
     */
    public void setSessions(Hashtable arg0) {
        throw new UnsupportedOperationException("NYI");
    }
    
}
