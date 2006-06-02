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
package org.codehaus.wadi.jetty5;

import java.io.InputStream;
import java.util.EventListener;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Manager;
import org.codehaus.wadi.ManagerConfig;
import org.codehaus.wadi.impl.SpringManagerFactory;
import org.codehaus.wadi.impl.StandardManager;
import org.codehaus.wadi.web.WADIHttpSession;
import org.codehaus.wadi.web.impl.AtomicallyReplicableSessionFactory;
import org.codehaus.wadi.web.impl.Filter;
import org.codehaus.wadi.web.impl.ListenerSupport;
import org.mortbay.jetty.servlet.Dispatcher;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.SessionManager;
import org.mortbay.jetty.servlet.WebApplicationHandler;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JettyManager implements ManagerConfig, SessionManager {

	protected final Log _log = LogFactory.getLog(getClass());

	protected final ListenerSupport _listeners=new ListenerSupport();

	protected StandardManager _wadi;
	protected ServletHandler _handler;
	protected boolean _secureCookies=false;
	protected boolean _httpOnly=true;
	protected boolean _useRequestedId=false;

	// org.codehaus.wadi.ManagerConfig

	public ServletContext getServletContext() {
		return _handler.getServletContext();
	}

	public void callback(Manager manager) {
		_listeners.installListeners((StandardManager)manager);
	}

	// org.mortbay.jetty.servlet.SessionManager

	public void initialize(ServletHandler handler) {
		_handler=handler;
		try {
			InputStream descriptor=_handler.getHttpContext().getResource("WEB-INF/wadi-web.xml").getInputStream();
			_wadi=(StandardManager)SpringManagerFactory.create(descriptor, "SessionManager", new AtomicallyReplicableSessionFactory(), new JettySessionWrapperFactory());
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
		org.codehaus.wadi.web.WebSession session = _wadi.create();
		if (false == session instanceof WADIHttpSession) {
			throw new IllegalStateException(WADIHttpSession.class + " is expected.");
		}
		WADIHttpSession httpSession = (WADIHttpSession) session;
		return httpSession.getWrapper();
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
		WebApplicationHandler handler=(WebApplicationHandler)_handler;
		String name="WadiFilter";
		handler.defineFilter(name, Filter.class.getName());;
		handler.addFilterPathMapping("/*", name, Dispatcher.__ALL);
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
