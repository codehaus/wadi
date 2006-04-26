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
import org.codehaus.wadi.ManagerConfig;
import org.codehaus.wadi.WADIHttpSession;
import org.codehaus.wadi.impl.AtomicallyReplicableSessionFactory;
import org.codehaus.wadi.impl.SpringManagerFactory;
import org.codehaus.wadi.impl.StandardManager;

// REQUIREMENTS:
// Lifecycle - start/stop

// ISSUES:
// activate/passivateSession - not needed


public class Axis2Manager implements SessionManager, ManagerConfig {
    
    protected final Log _log=LogFactory.getLog(getClass());
    protected final Axis2SessionWrapperFactory _wrapperFactory=new Axis2SessionWrapperFactory();

    // integrated
    protected int _maxInactiveInterval; // integrated
    protected StandardManager _wadi;

    // still to integrate
    protected SessionIdFactory _idFactory; // can we integrate this somehow ?
    protected long _checkInterval; // needs integration with Evicter ?
    protected int _maxActiveSessions; // needs integration with Evicter ?
    
    
    public void start() throws Exception {
        try {
            // we should probably acquire this through some backptr from the container...
            String path=System.getProperty("axis2.repo")+"/server/wadi-axis2.xml";
            InputStream descriptor=new FileInputStream(path);
            _wadi=(StandardManager)SpringManagerFactory.create(descriptor, "SessionManager", new AtomicallyReplicableSessionFactory(), new Axis2SessionWrapperFactory());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        _wadi.setMaxInactiveInterval(_maxInactiveInterval);
        _wadi.init(this);
        _wadi.start();
    }
        
    public void stop() throws Exception {
        _wadi.stop();   
    }

    public Axis2Manager() throws Exception {
        start();
    }
    
    // WADI ManagerConfig

    public ServletContext getServletContext() {
        return null; // Not relevant to Axis2
    }

    public void callback(StandardManager arg0) {
        _log.info("callback called");
    }
    
    // Axis2 SessionManager

    // Session lifecycle
    
    public Session findSession(String arg0) {
        throw new UnsupportedOperationException("NYI");
    }

    public Session createSession() {
        org.codehaus.wadi.Session session = _wadi.create();
        WADIHttpSession httpSession = (WADIHttpSession)session;
        return (Axis2Session)httpSession.getWrapper();
    }

    public void release(Session key) {
        _log.info("activateSession("+key+") - ignoring");
    }

    public void activateSession(String key) {
        _log.info("activateSession("+key+") - ignoring");
    }

    public void passivateSession(String key) {
        _log.info("passivateSession("+key+") - ignoring");
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
    
    public Hashtable getSessions() {
        throw new UnsupportedOperationException("NYI");
    }

    public void setSessions(Hashtable arg0) {
        throw new UnsupportedOperationException("NYI");
    }

}
