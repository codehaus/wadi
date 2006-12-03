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
package org.codehaus.wadi.jetty5;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.HttpHandler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.jetty.servlet.WebApplicationHandler;


/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
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
	protected final WebApplicationContext _context;
	protected final WebApplicationHandler _handler;
	protected final FilterHolder _filterHolder;
	protected final ServletHolder _servletHolder;
	protected final Filter _filter;
	protected final Servlet _servlet;
	protected final HttpHandler _whandler;

	public JettyNode(String name, String host, int port, String context, String webApp, Filter filter, Servlet servlet) throws Exception,IOException, UnknownHostException {
		System.setProperty("org.mortbay.xml.XmlParser.NotValidating", "true");
		_log=LogFactory.getLog(getClass().getName()+"#"+name);

		_context=_server.addWebApplication(context, webApp);
		_whandler=new Handler(Pattern.compile("127\\.0\\.0\\.1|192\\.168\\.0\\.\\d{1,3}"));
		_context.addHandler(0, _whandler);
		_context.start();
		HttpHandler[] handlers=_context.getHandlers();
		_handler=(WebApplicationHandler)handlers[1];
		// handler
		_filterHolder=_handler.getFilter("Filter");
		_servletHolder=_handler.getServletHolder("Servlet");

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
