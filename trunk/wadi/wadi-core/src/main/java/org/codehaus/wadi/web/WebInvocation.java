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
package org.codehaus.wadi.web;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.PoolableHttpServletRequestWrapper;
import org.codehaus.wadi.PoolableInvocationWrapper;

/**
 * @version $Revision: 1430 $
 */
public class WebInvocation implements Invocation {
    
    protected static ThreadLocal _threadLocalInstance=new ThreadLocal() {protected Object initialValue() {return new WebInvocation();}};
    
    public static WebInvocation getThreadLocalInstance() {
        return (WebInvocation)_threadLocalInstance.get();
    }
    
	public static final WebInvocation RELOCATED_INVOCATION = new WebInvocation();
	
	private HttpServletRequest hreq;
	private HttpServletResponse hres;
	private FilterChain chain;
	private boolean proxiedInvocation=true;
    
    public WebInvocation() {
    }
    
    public void init(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain) {
        this.hreq=hreq;
        this.hres=hres;
        this.chain=chain;
        this.proxiedInvocation=(null==hreq);
    }
    
    // Invocation
    
    public void clear() {
        hreq=null;
        hres=null;
        chain=null;
        proxiedInvocation=true;
    }
    
	public String getKey() {
	    return hreq.getRequestedSessionId();   
    }
    
    public void sendError(int code, String message) throws Exception {
        hres.sendError(code, message); // TODO - should we allow custom error page ?
    }
    
    public boolean getRelocatable() {
        return true;
    }
    
    // old
    
	public FilterChain getChain() {
		return chain;
	}
	
	public HttpServletRequest getHreq() {
		return hreq;
	}
	
	public HttpServletResponse getHres() {
		return hres;
	}
	
	public boolean isProxiedInvocation() {
		return proxiedInvocation;
	}
	
	public void invoke(PoolableInvocationWrapper wrapper) throws InvocationException {
		PoolableHttpServletRequestWrapper actualWrapper = (PoolableHttpServletRequestWrapper) wrapper;
		try {
			chain.doFilter(actualWrapper, hres);
		} catch (Exception e) {
			throw new InvocationException(e);
		}
	}
	
	public void invoke() throws InvocationException {
		try {
			chain.doFilter(hreq, hres);
		} catch (Exception e) {
			throw new InvocationException(e);
		}
	}
}
