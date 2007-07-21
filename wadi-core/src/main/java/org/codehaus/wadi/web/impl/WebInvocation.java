/**
 *
 * Copyright 2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.web.impl;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.contextualiser.InvocationContext;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.contextualiser.InvocationProxy;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.group.EndPoint;
import org.codehaus.wadi.web.HttpInvocationContext;

/**
 * @version $Revision: 1430 $
 */
public class WebInvocation implements Invocation {
    private HttpServletRequest hreq;
    private HttpServletResponse hres;
    private FilterChain chain;
    private boolean proxiedInvocation = true;
    private InvocationProxy proxy;
    private Session session;
    private boolean errorIfSessionNotAcquired;
    private long exclusiveSessionLockWaitTime;

    public WebInvocation() {
        this(5000);
    }
    
    public WebInvocation(long exclusiveSessionLockWaitTime) {
        this.exclusiveSessionLockWaitTime = exclusiveSessionLockWaitTime;
    }
    
    /**
     * Initialise this WebInvocation for action after being taken from a Pool
     *
     * @param hreq
     * @param hres
     * @param chain
     */
    public void init(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain) {
        this.hreq = hreq;
        this.hres = hres;
        this.chain = chain;
        this.proxiedInvocation = (null == hreq);
    }

    public void setInvocationProxy(InvocationProxy proxy) {
        this.proxy=proxy;
    }
    
    public String getSessionKey() {
        return hreq.getRequestedSessionId();
    }

    public void sendError(int code, String message) throws InvocationException {
        try {
            hres.sendError(code, message);
        } catch (IOException e) {
            throw new InvocationException("could not return error to client - " + code + " : " + message, e);
        }
    }

    public boolean isRelocatable() {
        return true;
    }

    public void relocate(EndPoint endPoint) throws InvocationException {
    	URIEndPoint wep=(URIEndPoint)endPoint;
        // make some decisions about how we would like to relocate the Invocation :
        // via redirection
        // via proxy
        // lets go for proxy first:
    	try {
    		proxy.proxy(wep, this);
    	} catch (ProxyingException e) {
    		throw new InvocationException("unexpected Invocation relocation failure: ", e);
    	}
        // thoughts :
        // will it be restuck at the other end ?
    }

    public boolean isProxiedInvocation() {
        return proxiedInvocation;
    }

    public void invoke(InvocationContext wrapper) throws InvocationException {
        HttpInvocationContext actualWrapper = (HttpInvocationContext) wrapper;
        try {
            chain.doFilter(actualWrapper, hres);
        } catch (Exception e) {
            throw new InvocationException(e);
        } finally {
            if (null != session) {
                session.onEndProcessing();
            }
        }
    }

    public void invoke() throws InvocationException {
        try {
            chain.doFilter(hreq, hres);
        } catch (Exception e) {
            throw new InvocationException(e);
        }
    }

    public FilterChain getChain() {
        return chain;
    }

    public HttpServletRequest getHreq() {
        return hreq;
    }

    public HttpServletResponse getHres() {
        return hres;
    }

    public Session getSession() {
        return session;
    }
    
    public void setSession(Session session) {
        this.session = session;
    }

    public boolean isErrorIfSessionNotAcquired() {
        return errorIfSessionNotAcquired;
    }

    public void setErrorIfSessionNotAcquired(boolean errorIfSessionNotAcquired) {
        this.errorIfSessionNotAcquired = errorIfSessionNotAcquired;
    }
    
    public long getExclusiveSessionLockWaitTime() {
        return exclusiveSessionLockWaitTime;
    }

    
}
