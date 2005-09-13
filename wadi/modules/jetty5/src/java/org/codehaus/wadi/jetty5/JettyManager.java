package org.codehaus.wadi.jetty5;

import java.io.InputStream;
import java.util.EventListener;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.ManagerConfig;
import org.codehaus.wadi.impl.DistributableManager;
import org.codehaus.wadi.impl.DistributableSessionFactory;
import org.codehaus.wadi.impl.ListenerSupport;
import org.codehaus.wadi.impl.SpringManagerFactory;
import org.codehaus.wadi.impl.StandardManager;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.SessionManager;

public class JettyManager implements ManagerConfig, SessionManager {

  protected final Log _log = LogFactory.getLog(getClass());

  protected final ListenerSupport _listeners=new ListenerSupport();

  protected DistributableManager _wadi;
  protected ServletHandler _handler;
  protected boolean _secureCookies=false;
  protected boolean _httpOnly=true;
  protected boolean _useRequestedId=false;

  // org.codehaus.wadi.ManagerConfig

  public ServletContext getServletContext() {
    return _handler.getServletContext();
  }

  public void callback(StandardManager manager) {
    _listeners.installListeners(manager);
  }

  // org.mortbay.jetty.servlet.SessionManager

  public void initialize(ServletHandler handler) {
    _handler=handler;
    try {
      InputStream descriptor=_handler.getHttpContext().getResource("WEB-INF/wadi-web.xml").getInputStream();
      _wadi=(DistributableManager)SpringManagerFactory.create(descriptor, "SessionManager", new DistributableSessionFactory(), new JettySessionWrapperFactory(), new JettyManagerFactory());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    _wadi.init(this);
  }

  public HttpSession getHttpSession(String id) {
    //throw new UnsupportedOperationException();
    return null; // FIXME - this will be the container trying to 'refresh' a session...
  }

  public HttpSession newHttpSession(HttpServletRequest request) {
    return _wadi.create().getWrapper();
  }

  public boolean getSecureCookies() {
    return _secureCookies;
  }

  public boolean getHttpOnly() {
    return _httpOnly;
  }

  public int getMaxInactiveInterval() {
    return _wadi.getMaxInactiveInterval();
  }

  public void setMaxInactiveInterval(int seconds) {
    _wadi.setMaxInactiveInterval(seconds);
  }

  public void addEventListener(EventListener listener) throws IllegalArgumentException {
    _listeners.addEventListener(listener);
  }

  public void removeEventListener(EventListener listener) {
    _listeners.removeEventListener(listener);
  }

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

  public void start() throws Exception {
    _wadi.start();
  }

  public void stop() throws InterruptedException {
    try {
      _wadi.stop();
    } catch (Exception e) {
      _log.warn("unexpected problem shutting down", e);
    }
  }

  public boolean isStarted() {
    return _wadi.isStarted();
  }

  // org.codehaus.wadi.jetty5.JettyManager

  protected boolean getUseRequestedId() {
    return _useRequestedId;
  }

}
