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
package org.codehaus.wadi.sandbox.impl;

import java.io.BufferedReader;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Since HttpServletRequestWrapper insists on having a valid delegate at
 * all times, we need a dummy to use in our cached ThreadLocal whilst it is
 * not being used - yeugh !
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class DummyHttpServletRequest implements HttpServletRequest {
	protected static final RuntimeException _exception=new UnsupportedOperationException();

	public String getAuthType(){throw _exception;}
	public Cookie[] getCookies(){throw _exception;}
	public long getDateHeader(String name){throw _exception;}
	public String getHeader(String name){throw _exception;}
	public Enumeration getHeaders(String name){throw _exception;}
	public Enumeration getHeaderNames(){throw _exception;}
	public int getIntHeader(String name){throw _exception;}
	public String getMethod(){throw _exception;}
	public String getPathInfo(){throw _exception;}
	public String getPathTranslated(){throw _exception;}
	public String getContextPath(){throw _exception;}
	public String getQueryString(){throw _exception;}
	public String getRemoteUser(){throw _exception;}
	public boolean isUserInRole(String role){throw _exception;}
	public Principal getUserPrincipal(){throw _exception;}
	public String getRequestedSessionId(){throw _exception;}
	public String getRequestURI(){throw _exception;}
	public StringBuffer getRequestURL(){throw _exception;}
	public String getServletPath(){throw _exception;}
	public HttpSession getSession(boolean create){throw _exception;}
	public HttpSession getSession(){throw _exception;}
	public boolean isRequestedSessionIdValid(){throw _exception;}
	public boolean isRequestedSessionIdFromCookie(){throw _exception;}
	public boolean isRequestedSessionIdFromURL(){throw _exception;}
	public boolean isRequestedSessionIdFromUrl(){throw _exception;}
	public Object getAttribute(String name){throw _exception;}
	public Enumeration getAttributeNames(){throw _exception;}
	public String getCharacterEncoding(){throw _exception;}
	public void setCharacterEncoding(String en){throw _exception;}
	public int getContentLength(){throw _exception;}
	public String getContentType(){throw _exception;}
	public ServletInputStream getInputStream(){throw _exception;}
	public String getParameter(String name){throw _exception;}
	public Enumeration getParameterNames(){throw _exception;}
	public String[] getParameterValues(String name){throw _exception;}
	public Map getParameterMap(){throw _exception;}
	public String getProtocol(){throw _exception;}
	public String getScheme(){throw _exception;}
	public String getServerName(){throw _exception;}
	public int getServerPort(){throw _exception;}
	public BufferedReader getReader(){throw _exception;}
	public String getRemoteAddr(){throw _exception;}
	public String getRemoteHost(){throw _exception;}
	public void setAttribute(String name, Object o){throw _exception;}
	public void removeAttribute(String name){throw _exception;}
	public Locale getLocale(){throw _exception;}
	public Enumeration getLocales(){throw _exception;}
	public boolean isSecure(){throw _exception;}
	public RequestDispatcher getRequestDispatcher(String path){throw _exception;}
	public String getRealPath(String path){throw _exception;}
	public int getRemotePort(){throw _exception;}
	public String getLocalName(){throw _exception;}
	public String getLocalAddr(){throw _exception;}
	public int getLocalPort(){throw _exception;}
}
