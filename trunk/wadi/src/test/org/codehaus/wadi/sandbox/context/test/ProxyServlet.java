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

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.HttpProxy;
import org.codehaus.wadi.sandbox.context.impl.StandardHttpProxy;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHttpContext;
import org.mortbay.util.InetAddrPort;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ProxyServlet implements Servlet {
	protected final Log _log = LogFactory.getLog(getClass());

	protected ServletConfig _config;
	protected ServletContext _context;

	protected HttpProxy _proxy=new StandardHttpProxy();

	public void init(ServletConfig config) throws ServletException {
		_config = config;
		_context = config.getServletContext();
	}

	public ServletConfig getServletConfig() {
		return _config;
	}

	public void service(ServletRequest req, ServletResponse res)
	throws ServletException, IOException {

		HttpServletRequest hreq=(HttpServletRequest)req;

		if (!_proxy.canProxy(req)) {
			_log.info("request not proxyable: "+hreq.getRequestURL());
			// so we can't do anything about it...
			return;
		}

		if (!_proxy.isStateful(hreq)) {
			_log.warn("request not stateful: "+hreq.getMethod()+" - "+hreq.getRequestURI());
			// still needs to be proxied as we don't have the webapp here...
		}

		String uri=hreq.getRequestURI();
		String qs=hreq.getQueryString();
		if (qs!=null) {
			uri=new StringBuffer(uri).append("?").append(qs).toString();
		}

		URL url=new URL(req.getScheme(), req.getServerName(), req.getServerPort(), uri);
		_proxy.proxy(req, res, url); // TODO - why a URL - expensive...
	}

	public String getServletInfo() {
		return "Proxy Servlet";
	}

	public void destroy() {
	}

	public static void main(String[] args) throws Exception {
		Server server=new Server();
		SocketListener listener=new SocketListener(new InetAddrPort(8080));
		listener.setMinThreads(1000);
		listener.setMaxThreads(2000);
		server.addListener(listener);
		ServletHttpContext context=(ServletHttpContext)server.getContext("/");
		context.addServlet("Proxy", "/", ProxyServlet.class.getName());
		server.start();
	}
}
