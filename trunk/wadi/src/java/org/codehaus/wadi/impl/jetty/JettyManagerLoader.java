package org.codehaus.wadi.impl.jetty;

import java.io.InputStream;
import java.util.EventListener;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.DistributableSessionFactory;
import org.codehaus.wadi.impl.SpringManagerFactory;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.SessionManager;
import org.mortbay.jetty.servlet.WebApplicationContext;

public class JettyManagerLoader implements SessionManager {
	
	protected final Log _log = LogFactory.getLog(getClass());
	
	protected JettyManager _peer;
	
	public void init(WebApplicationContext context) {
		try {
			InputStream descriptor=context.getResource("WEB-INF/wadi-web.xml").getInputStream();
			_peer=(JettyManager)SpringManagerFactory.create(descriptor, "SessionManager", new DistributableSessionFactory(), new JettySessionWrapperFactory(), new JettyManagerFactory());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// org.mortbay.jetty.servlet.SessionManager
	
	public void initialize(ServletHandler handler) {
		try {
			WebApplicationContext context=(WebApplicationContext)handler.getHttpContext();
			init(context);
		} catch (Exception e) {
			_log.error("unexpected problem initialising SessionManager", e);
		}
		_peer.initialize(handler);
	}
	
	public HttpSession getHttpSession(String id) {
		return _peer.getHttpSession(id);
	}
	
	public HttpSession newHttpSession(HttpServletRequest request) {
		return _peer.newHttpSession(request);
	}
	
	public boolean getSecureCookies() {
		return _peer.getSecureCookies();
	}
	
	public boolean getHttpOnly() {
		return _peer.getHttpOnly();
	}
	
	public int getMaxInactiveInterval() {
		return _peer.getMaxInactiveInterval();
	}
	
	public void setMaxInactiveInterval(int seconds) {
		_peer.setMaxInactiveInterval(seconds);
	}
	
	public void addEventListener(EventListener listener) {
		_peer.addEventListener(listener);
	}
	
	public void removeEventListener(EventListener listener) {
		_peer.removeEventListener(listener);
	}
	
	public Cookie getSessionCookie(HttpSession session, boolean requestIsSecure) {
		return _peer.getSessionCookie(session, requestIsSecure);
	}
	
	public void start() throws Exception {
		_peer.start();
	}
	
	public void stop() throws InterruptedException {
		_peer.stop();
	}
	
	public boolean isStarted() {
		return _peer.isStarted();
	}
	
}
