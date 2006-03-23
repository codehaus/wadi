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

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.jetty.servlet.WebApplicationHandler;

import junit.framework.TestCase;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class TestJetty extends TestCase {
	protected Log _log;
	protected Server _server=new Server();
	protected SocketListener _listener=new SocketListener();
	protected WebApplicationContext _context=new WebApplicationContext();
	protected WebApplicationHandler _handler=new WebApplicationHandler();
	protected ServletHolder _servletHolder;
	protected Servlet _servlet;

	public class TestServlet implements Servlet {

	    public void init(ServletConfig config) {
	        // nothing to do
	    }

	    public ServletConfig getServletConfig() {return null;}

	    public String getServletInfo() {return null;}

	    public void destroy() {
	        // nothing to do
	    }

	    public void service(ServletRequest req, ServletResponse res) {
	        HttpServletRequest hreq=(HttpServletRequest)req;
	        HttpServletResponse hres=(HttpServletResponse)res;
	        String name=hreq.getPathInfo();
	        name=name.substring(1, name.length());
            if (_log.isInfoEnabled()) _log.info("invoking: "+name);
            Class[] argTypes=new Class[]{HttpServletRequest.class, HttpServletResponse.class};
	        Object[] argInstances=new Object[]{hreq, hres};
	        try {
	            TestJetty.class.getMethod(name, argTypes).invoke(TestJetty.this, argInstances);
	        } catch (Exception e) {
		  _log.error(e);
                assertTrue(false);
	        }
	    }
	}

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		_log=LogFactory.getLog(getClass().getName());
		//servlet
		String servletName="Servlet";
		_servletHolder=new ServletHolder(_handler, servletName, ServletInstance.class.getName());
		_handler.addServletHolder(_servletHolder);
		_handler.mapPathToServlet("/*", servletName);
		// handler
		_context.addHandler(_handler);
		// context
		_context.setContextPath("/");
		_server.addContext(_context);
		// listener
		_listener.setHost("localhost");
		_listener.setPort(8080);
		_server.addListener(_listener);

		_servlet=new TestServlet();
		System.setProperty("org.mortbay.xml.XmlParser.NotValidating", "true");
		_server.start();

		((ServletInstance)_servletHolder.getServlet()).setInstance(_servlet);
	}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();

		_server.stop();
	}

	/**
	 * Constructor for TestJetty.
	 * @param name
	 */
	public TestJetty(String name) {
		super(name);
	}

	// how exactly are headers handled by Jetty ? (and hopefully Tomcat)
	public void testHeaders() throws Exception {
		HttpURLConnection huc=(HttpURLConnection)new URL("http://localhost:8080/testHeaders").openConnection();
		// set headers
		huc.addRequestProperty("A","1");
		huc.addRequestProperty("a","2");
		huc.addRequestProperty("b","2");
		huc.addRequestProperty("c","3");

		huc.connect();
		assertTrue(huc.getResponseCode()==200);
	}

	public void testHeaders(HttpServletRequest hreq, HttpServletResponse hres) {

        if (_log.isInfoEnabled()) _log.info("HttpServletRequest.class is: "+hreq.getClass().getName());

        for (Enumeration e=hreq.getHeaderNames(); e.hasMoreElements(); ) {
			String key=(String)e.nextElement();
			for (Enumeration f=hreq.getHeaders(key); f.hasMoreElements(); ) {
				String val=(String)f.nextElement();
                if (_log.isInfoEnabled()) _log.info(key+":"+val);
            }
		}

		for (Enumeration e=hreq.getHeaderNames(); e.hasMoreElements(); ) {
			String key=(String)e.nextElement();
			String val=hreq.getHeader(key);
            if (_log.isInfoEnabled()) _log.info(key+":"+val);
        }
	}
}
