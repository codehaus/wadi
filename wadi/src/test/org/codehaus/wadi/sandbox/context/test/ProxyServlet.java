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
import java.net.InetSocketAddress;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

	protected HttpProxy _proxy=new StandardHttpProxy("jsessionid");

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
		HttpServletResponse hres=(HttpServletResponse)res;

//		if (!_proxy.canProxy(hreq)) {
//			_log.info("request not proxyable: "+hreq.getRequestURL());
//			// so we can't do anything about it...
//			return;
//		}

		InetSocketAddress location=null;
		try {
			location=new InetSocketAddress(req.getServerName(), req.getServerPort());
			if (!_proxy.proxy(location, hreq, hres))
				_log.warn("request not stateful: "+hreq.getMethod()+" - "+hreq.getRequestURI());
		} catch (Exception e) {
			hres.setHeader("Date", null);
			hres.setHeader("Server", null);
			hres.addHeader("Via", "1.1 (WADI)");
			// hres.setStatus(502, message);
			try {
				_log.warn("could not establish connection to location: "+location, e);
				hres.sendError(502, "Bad Gateway: proxy could not establish connection to server"); // TODO - why do we need to use sendError ?
			} catch (IOException e2) {
				_log.warn("could not return error to client", e2);
			}
		}
	}

	public String getServletInfo() {
		return "Proxy Servlet";
	}

	public void destroy() {
	}

	public static class RemoteDetailsServlet implements Servlet {
		protected final Log _log = LogFactory.getLog(getClass());
		public void init(ServletConfig config) throws ServletException {}
		public ServletConfig getServletConfig() {return null;}
		public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
			HttpServletRequest hreq=(HttpServletRequest)req;
			HttpServletResponse hres=(HttpServletResponse)res;
			_log.info("Via: "+hreq.getHeader("Via"));
			_log.info("Max-Forwards: "+hreq.getHeader("Max-Forwards"));
			_log.info("X-Forwarded-For: "+hreq.getHeader("X-Forwarded-For"));
			_log.info("remote location was: "+req.getRemoteAddr()+"/"+req.getRemoteHost()+":"+req.getRemotePort());
		}
		public String getServletInfo() {return null;}
		public void destroy() {}
	}

	public static void main(String[] args) throws Exception {
		{
			Server server=new Server();
			SocketListener listener=new SocketListener(new InetAddrPort(8080));
			listener.setMinThreads(1);
			//listener.setMaxThreads(2000);
			server.addListener(listener);
			ServletHttpContext context=(ServletHttpContext)server.getContext("/");
			context.addServlet("Proxy", "/", ProxyServlet.class.getName());
			server.start();
		}
		{
			Server server=new Server();
			SocketListener listener=new SocketListener(new InetAddrPort(8081));
			listener.setMinThreads(1);
			//listener.setMaxThreads(2000);
			server.addListener(listener);
			ServletHttpContext context=(ServletHttpContext)server.getContext("/");
			context.addServlet("RemoteDetails", "/", RemoteDetailsServlet.class.getName());
			server.start();
		}
	}
}
