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
package org.codehaus.wadi.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class MyHttpServletRequest implements HttpServletRequest {

    public MyHttpServletRequest() {
        super();
        // TODO Auto-generated constructor stub
    }

    public String getAuthType() {
        // TODO Auto-generated method stub
        return null;
    }

    public Cookie[] getCookies() {
        // TODO Auto-generated method stub
        return null;
    }

    public long getDateHeader(String name) {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getHeader(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public Enumeration getHeaders(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public Enumeration getHeaderNames() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getIntHeader(String name) {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getMethod() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getPathInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getPathTranslated() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getContextPath() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getQueryString() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getRemoteUser() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isUserInRole(String role) {
        // TODO Auto-generated method stub
        return false;
    }

    public Principal getUserPrincipal() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getRequestedSessionId() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getRequestURI() {
        // TODO Auto-generated method stub
        return null;
    }

    public StringBuffer getRequestURL() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getServletPath() {
        // TODO Auto-generated method stub
        return null;
    }

    public HttpSession getSession(boolean create) {
        // TODO Auto-generated method stub
        return null;
    }

    public HttpSession getSession() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isRequestedSessionIdValid() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isRequestedSessionIdFromCookie() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isRequestedSessionIdFromURL() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isRequestedSessionIdFromUrl() {
        // TODO Auto-generated method stub
        return false;
    }

    public Object getAttribute(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public Enumeration getAttributeNames() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getCharacterEncoding() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setCharacterEncoding(String env)
            throws UnsupportedEncodingException {
        // TODO Auto-generated method stub

    }

    public int getContentLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getContentType() {
        // TODO Auto-generated method stub
        return null;
    }

    public ServletInputStream getInputStream() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getParameter(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public Enumeration getParameterNames() {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getParameterValues(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public Map getParameterMap() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getProtocol() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getScheme() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getServerName() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getServerPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    public BufferedReader getReader() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getRemoteAddr() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getRemoteHost() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setAttribute(String name, Object o) {
        // TODO Auto-generated method stub

    }

    public void removeAttribute(String name) {
        // TODO Auto-generated method stub

    }

    public Locale getLocale() {
        // TODO Auto-generated method stub
        return null;
    }

    public Enumeration getLocales() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isSecure() {
        // TODO Auto-generated method stub
        return false;
    }

    public RequestDispatcher getRequestDispatcher(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getRealPath(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    public int getRemotePort() {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getLocalName() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getLocalAddr() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getLocalPort() {
        // TODO Auto-generated method stub
        return 0;
    }

}
