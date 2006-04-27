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

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.InvocationProxy;
import org.codehaus.wadi.ProxiedLocation;
import org.codehaus.wadi.impl.StandardHttpProxy;
import org.codehaus.wadi.web.HTTPProxiedLocation;
import org.codehaus.wadi.web.WebInvocation;
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
	
	protected InvocationProxy _proxy=new StandardHttpProxy("jsessionid");
	
	public void init(ServletConfig config) {
		_config = config;
		_context = config.getServletContext();
	}
	
	public ServletConfig getServletConfig() {
		return _config;
	}
	
	public void service(ServletRequest req, ServletResponse res) {
		
		HttpServletRequest hreq=(HttpServletRequest)req;
		HttpServletResponse hres=(HttpServletResponse)res;
		
//		if (!_proxy.canProxy(hreq)) {
//		_log.info("request not proxyable: "+hreq.getRequestURL());
//		// so we can't do anything about it...
//		return;
//		}
		
		ProxiedLocation location=null;
		try {
			location=new HTTPProxiedLocation(new InetSocketAddress(req.getServerName(), req.getServerPort()));
			WebInvocation invocation=new WebInvocation();
			invocation.init(hreq, hres, null);
			_proxy.proxy(location, invocation);
		} catch (Exception e) {
			hres.setHeader("Date", null);
			hres.setHeader("Server", null);
			hres.addHeader("Via", "1.1 (WADI)");
			// hres.setStatus(502, message);
			try {
				String message="problem proxying request to; "+location;
				_log.warn(message, e);
				hres.sendError(502, "Bad Gateway: "+message);
			} catch (IOException e2) {
				_log.warn("could not return error to client", e2);
			}
		}
	}
	
	public String getServletInfo() {
		return "Proxy Servlet";
	}
	
	public void destroy() {
		_context=null;
		_config=null;
	}
	
	public static class RemoteDetailsServlet implements Servlet {
		
		protected final Log _log = LogFactory.getLog(getClass());
		
		public void init(ServletConfig config) {
			// nothing to do
		}
		
		public ServletConfig getServletConfig() {return null;}
		
		public void service(ServletRequest req, ServletResponse res) {
			HttpServletRequest hreq=(HttpServletRequest)req;
			if (_log.isInfoEnabled()) {
				_log.info("Via: "+hreq.getHeader("Via"));
				_log.info("Max-Forwards: "+hreq.getHeader("Max-Forwards"));
				_log.info("X-Forwarded-For: "+hreq.getHeader("X-Forwarded-For"));
				_log.info("remote location was: "+req.getRemoteAddr()+"/"+req.getRemoteHost()+":"+req.getRemotePort());
			}
		}
		
		public String getServletInfo() {return null;}
		
		public void destroy() {
			// nothing to do
		}
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
