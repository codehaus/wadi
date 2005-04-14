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
package org.codehaus.wadi.sandbox.impl.jetty;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.codehaus.wadi.impl.TomcatIdGenerator;
import org.codehaus.wadi.sandbox.AttributesPool;
import org.codehaus.wadi.sandbox.SessionPool;
import org.codehaus.wadi.sandbox.ValuePool;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.SessionManager;

public class Manager extends org.codehaus.wadi.sandbox.impl.Manager implements SessionManager {

    public Manager(SessionPool sessionPool, AttributesPool attributesPool, ValuePool valuePool) {
        super(sessionPool, attributesPool, valuePool, new SessionWrapperFactory(), new TomcatIdGenerator());
    }

    public void initialize(ServletHandler handler) {
        // TODO Auto-generated method stub

    }

    public HttpSession getHttpSession(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    public HttpSession newHttpSession(HttpServletRequest request) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean getSecureCookies() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean getHttpOnly() {
        // TODO Auto-generated method stub
        return false;
    }

    public Cookie getSessionCookie(HttpSession session, boolean requestIsSecure) {
        // TODO Auto-generated method stub
        return null;
    }

    public void start() throws Exception {
        // TODO Auto-generated method stub

    }

    public void stop() throws InterruptedException {
        // TODO Auto-generated method stub

    }
    
}
