/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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
package org.codehaus.wadi.sandbox.context.test;

import java.io.IOException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.wadi.sandbox.context.HttpProxy;
import org.codehaus.wadi.sandbox.context.impl.StandardHttpProxy;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class TestProxy extends TestServlet{

	public class ProxyServlet extends HttpServlet {
		public void service(ServletRequest req, ServletResponse res)
				throws ServletException, IOException {
			_log.info("session-id: "+((HttpServletRequest)req).getRequestedSessionId());
			new StandardHttpProxy().proxy(req, res, new URL("http://localhost:8080/test/admin"));
		}
	}

	public class ProxyFilter implements Filter {
		public void init(FilterConfig config){
		}

		public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
				throws ServletException, IOException {
			new StandardHttpProxy().proxy(req, res, new URL("http://localhost:8080/test/admin"));
		}

		public void destroy(){
		}
	}

	protected void setUp() throws Exception {
		super.setUp();
		_log.info("setting up");
		add("Proxy", "/test", "/proxy", new ProxyServlet());
		add("Admin", "/test", "/admin", new org.mortbay.servlet.AdminServlet());
		start("localhost", 8080);
		}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		_log.info("tearing down");
		super.tearDown();
	}

	/**
	 * Constructor for TestProxy.
	 *
	 * @param arg0
	 */
	public TestProxy(String arg0) {
		super(arg0);
	}

	public void testGET() throws Exception {
		HttpMethod get=new GetMethod("http://localhost:8080");
		get.setPath("/test/proxy;jsessionid=xxx");
		HttpClient client=new HttpClient();
		client.executeMethod(get);
		String proxied=get.getResponseBodyAsString();
		_log.info(proxied);
		get.recycle();
		get.setPath("/test/admin");
		client.executeMethod(get);
		String direct=get.getResponseBodyAsString();
		_log.info(direct);
		// assertTrue(direct.equals(proxied));
		//		Thread.sleep(60*1000);
	}
	
	public void testRegexps() {
		HttpProxy proxy=new StandardHttpProxy();
		
		org.codehaus.wadi.test.container.HttpServletRequest hreq=new org.codehaus.wadi.test.container.HttpServletRequest();
		hreq.setScheme("http");
		assertTrue(proxy.canProxy(hreq));
		hreq.setScheme("HTTP");
		assertTrue(proxy.canProxy(hreq));
		hreq.setScheme("https");
		assertTrue(!proxy.canProxy(hreq));
		hreq.setScheme("HTTPS");
		assertTrue(!proxy.canProxy(hreq));
		
//		hreq.setRequestedSessionId(null);
		hreq.setMethod("GET");
		hreq.setRequestURI("/foo/bar");
//		assertTrue(!proxy.isStateful(hreq));
		
		hreq.setRequestedSessionId("xxx");
		assertTrue(proxy.isStateful(hreq));
		
		hreq.setMethod("POST");
		assertTrue(proxy.isStateful(hreq));
		hreq.setMethod("get");
		assertTrue(proxy.isStateful(hreq));
		hreq.setMethod("post");
		assertTrue(proxy.isStateful(hreq));
		hreq.setMethod("CONNECT");
		assertTrue(!proxy.isStateful(hreq));
		
		hreq.setMethod("GET");
		hreq.setRequestURI("/foo/bar.gif");
		assertTrue(!proxy.isStateful(hreq));
		
		hreq.setRequestURI("/search/search.dll.ico");
		assertTrue(!proxy.isStateful(hreq));
		hreq.setRequestURI("/cosnell_W0QQfsopZ1QQftsZ2QQsaatcZ3QQsatitleZQ22cosnellQ22.JPEG");
		assertTrue(!proxy.isStateful(hreq));
		hreq.setRequestURI("/cgi-bin/honesty-counter.cgi.PNg");
		assertTrue(!proxy.isStateful(hreq));
		hreq.setRequestURI("viewad/817-grey.gif");
		assertTrue(!proxy.isStateful(hreq));
	}

}
