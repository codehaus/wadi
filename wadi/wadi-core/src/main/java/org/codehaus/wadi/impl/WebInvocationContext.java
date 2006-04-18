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
package org.codehaus.wadi.impl;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.wadi.InvocationContext;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.PoolableHttpServletRequestWrapper;
import org.codehaus.wadi.PoolableInvocationWrapper;

/**
 * @version $Revision$
 */
public class WebInvocationContext implements InvocationContext {
	public static final WebInvocationContext RELOCATED_INVOCATION = new WebInvocationContext(null, null, null);
	
	private final HttpServletRequest hreq;
	private final HttpServletResponse hres;
	private final FilterChain chain;
	private final boolean proxiedInvocation;
	
	public WebInvocationContext(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain) {
		this.hreq = hreq;
		this.hres = hres;
		this.chain = chain;
		if (null == hreq) {
			proxiedInvocation = true;
		} else {
			proxiedInvocation = false;
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
