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

import java.net.UnknownHostException;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.impl.jetty.Handler;
import org.codehaus.wadi.sandbox.context.impl.jetty.SocketListener;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.SecurityConstraint;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.jetty.servlet.WebApplicationHandler;


public class JettyNode implements Node {

	public static class SwitchableListener extends SocketListener {
		protected boolean _secure;
		public boolean getSecure(){return _secure;}
		public void setSecure(boolean secure){_secure=secure;}
	
		public boolean isIntegral(org.mortbay.http.HttpConnection connection){return _secure||super.isIntegral(connection);}
	    public boolean isConfidential(org.mortbay.http.HttpConnection connection){return _secure||super.isConfidential(connection);}
	}

	protected final Log _log;
	protected final Server _server=new Server();
	protected final SwitchableListener _listener=new SwitchableListener();
	protected final WebApplicationContext _context=new WebApplicationContext();
	protected final WebApplicationHandler _handler=new WebApplicationHandler();
	protected final FilterHolder _filterHolder;
	protected final ServletHolder _servletHolder;
	protected final Filter _filter;
	protected final Servlet _servlet;
	protected final HttpHandler _whandler;
	
	public JettyNode(String name, String host, int port, String context, String pathSpec, Filter filter, Servlet servlet) throws UnknownHostException {
		_log=LogFactory.getLog(getClass().getName()+"#"+name);
		// filter
		String filterName="Filter";
		_filterHolder=new FilterHolder(_handler, filterName, FilterInstance.class.getName());
		_handler.addFilterHolder(_filterHolder);
		_handler.addFilterPathMapping(pathSpec, filterName, FilterHolder.__REQUEST);
		//servlet
		String servletName="Servlet";
		_servletHolder=new ServletHolder(_handler, servletName, ServletInstance.class.getName());
		_handler.addServletHolder(_servletHolder);
		_handler.mapPathToServlet(pathSpec, servletName);
		// handler
		_context.addHandler(_handler);
		// context
		_context.setContextPath(context);
		
		// security - any resource mapped below /confidential requires confidential transport
		SecurityConstraint sc=new SecurityConstraint();
		sc.setDataConstraint(SecurityConstraint.DC_CONFIDENTIAL);
		_context.addSecurityConstraint("/confidential/*", sc);
		
		// handler
		_whandler=new Handler(Pattern.compile("127\\.0\\.0\\.1|192\\.168\\.0\\.\\d{1,3}"));
		_context.addHandler(0, _whandler);			
		
		_server.addContext(_context);
		// listener
		_listener.setHost(host);
		_listener.setPort(port);
		_server.addListener(_listener);
		_filter=filter;
		_servlet=servlet;
	}

	public Filter getFilter(){return _filter;}
	public Servlet getServlet(){return _servlet;}
	
	public void start() throws Exception {
		_server.start();
		((FilterInstance)_filterHolder.getFilter()).setInstance(_filter);
		((ServletInstance)_servletHolder.getServlet()).setInstance(_servlet);
	}
	
	public void stop() throws Exception {
		_server.stop();
	}

	// Securable
	public boolean getSecure(){return _listener.getSecure();}
	public void setSecure(boolean secure){_listener.setSecure(secure);}
}