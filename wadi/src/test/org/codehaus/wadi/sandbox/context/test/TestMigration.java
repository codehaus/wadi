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
import java.net.UnknownHostException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.jetty.servlet.WebApplicationHandler;

import junit.framework.TestCase;

/**
 * Unit Tests requiring a pair of Jetty's. Each one is set up with a Filter and Servlet placeholder.
 * These are injected with actual Filter and Servlet instances before the running of each test. This
 * allows the tests to set up the innards of these components, make http requests to them and then inspect
 * their innards for the expected changes,
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class TestMigration extends TestCase {
	protected Log _log = LogFactory.getLog(getClass());
	
	class Node {
		protected Server _server=new Server();
		protected SocketListener _listener=new SocketListener();
		protected WebApplicationContext _context=new WebApplicationContext();
		protected WebApplicationHandler _handler=new WebApplicationHandler();
		protected FilterHolder _filterHolder;
		protected ServletHolder _servletHolder;
		
		public Node(String host, int port, String context, String pathSpec) throws UnknownHostException {
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
			_server.addContext(_context);
			// listener
			_listener.setHost(host);
			_listener.setPort(port);
			_server.addListener(_listener);
		}
		
		public void setFilterInstance(Filter instance) throws ServletException {
			((FilterInstance)_filterHolder.getFilter()).setInstance(instance);
		}
		
		public void setServletInstance(Servlet instance) throws ServletException {
			((ServletInstance)_servletHolder.getServlet()).setInstance(instance);
		}
	}
	
	public class TestFilter implements Filter {
		
		public void init(FilterConfig config) throws ServletException {
			_log.info("Filter.init()");
		}
		
		public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
		throws ServletException, IOException {
			_log.info("Filter.doFilter()");
			chain.doFilter(req, res);
		}
		
		public void destroy() {}
	}
	
	public class TestServlet implements Servlet {
		protected ServletConfig _config;
		
		public void init(ServletConfig config) throws ServletException {
			_config = config;
			_log.info("Servlet.init()");
		}

		public ServletConfig getServletConfig() {
			return _config;
		}

		public void service(ServletRequest req, ServletResponse res)
				throws ServletException, IOException {
			_log.info("Servlet.service()");
		}

		public String getServletInfo() {
			return "Test Servlet";
		}

		public void destroy() {
		}	
	}
	
	protected Node _node1;
	protected Node _node2;
	
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		System.setProperty("org.mortbay.xml.XmlParser.NotValidating", "true");
		(_node1=new Node("localhost", 8080, "/test", "/*"))._server.start();
		(_node2=new Node("localhost", 8081, "/test", "/*"))._server.start();
		
		_node1.setFilterInstance(new TestFilter());
		_node1.setServletInstance(new TestServlet());
		
		_node2.setFilterInstance(new TestFilter());
		_node2.setServletInstance(new TestServlet());
	}
	
	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		_node2._server.stop();
		_node1._server.stop();
		super.tearDown();
	}
	
	/**
	 * Constructor for TestMigration.
	 * @param name
	 */
	public TestMigration(String name) {
		super(name);
	}

	public String get(HttpClient client, HttpMethod method, String path) throws IOException, HttpException {
		method.recycle();
		method.setPath(path);
		client.executeMethod(method);
		return method.getResponseBodyAsString();
	}
	
	public void testMigration() throws Exception {
		HttpClient client=new HttpClient();
		HttpMethod method=new GetMethod("http://localhost:8080");
		String result=get(client, method, "/test");
		_log.info("-"+result+"-");
		assertTrue(result.equals(""));
	}	
}
