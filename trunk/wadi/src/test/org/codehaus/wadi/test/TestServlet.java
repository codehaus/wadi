/*
 * Created on Feb 11, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletHttpContext;

import junit.framework.TestCase;

/**
 * @author jules
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */

public abstract class TestServlet extends TestCase {
	public static class ServletWrapper implements Servlet {

		protected ServletConfig _config;
		protected ServletContext _context;
		protected Servlet _servlet;

		public void init(ServletConfig config) throws ServletException {
			_config = config;
			_context = config.getServletContext();
		}

		public ServletConfig getServletConfig() {
			return _config;
		}

		public void service(ServletRequest req, ServletResponse res)
				throws ServletException, IOException {
			_servlet.service(req, res);
		}

		public String getServletInfo() {
			return "Test Servlet";
		}

		public void destroy() {
		}
		
		public void setServlet(Servlet servlet) throws ServletException {
			_servlet=servlet;
			_servlet.init(_config);
		}
		
		public Servlet getServlet(){return _servlet;}
	}
	
	protected Log _log = LogFactory.getLog(TestServlet.class);
	protected Server _server=new Server();
	protected SocketListener _listener=new SocketListener();

	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		_log.info("setting up");
		}

	protected Map _map=new HashMap();
	public synchronized void addServlet(String name, String context, String path, Servlet servlet) {
		Object[] tuple=new Object[]{name, path, servlet};
		List servlets=(List)_map.get(context);
		if (servlets==null)
			servlets=new ArrayList();
		servlets.add(tuple);
		_map.put(context, servlets);
		}

	public void start(String host, int port)
	throws Exception
	{
		_listener.setHost(host);
		_listener.setPort(port);
		_server.addListener(_listener);
		
		// walk through entries first time setting up and starting server
		for (Iterator i=_map.entrySet().iterator(); i.hasNext();)
		{
			Map.Entry e=(Map.Entry)i.next();
			String context=(String)e.getKey();
			List servlets=(List)e.getValue();

			ServletHttpContext c=(ServletHttpContext)_server.getContext(context);
			
			for (Iterator j=servlets.iterator(); j.hasNext();){
				Object[] tuple=(Object[])j.next();
				c.addServlet((String)tuple[0], (String)tuple[1], ServletWrapper.class.getName());
			}

			ServletHandler handler=(ServletHandler)c.getHandler(ServletHandler.class);
			handler.setAutoInitializeServlets(true);
		}
		
		_server.start();
		
		// walk back through and poke all the servlet instances into waiting wrappers...
		for (Iterator i=_map.entrySet().iterator(); i.hasNext();)
		{
			Map.Entry e=(Map.Entry)i.next();
			String context=(String)e.getKey();
			List servlets=(List)e.getValue();
			ServletHttpContext c=(ServletHttpContext)_server.getContext(context);
			ServletHandler handler=(ServletHandler)c.getHandler(ServletHandler.class);
			
			for (Iterator j=servlets.iterator(); j.hasNext();){
				Object[] tuple=(Object[])j.next();
				ServletHolder holder=handler.getServletHolder((String)tuple[0]);
				Servlet servlet=holder.getServlet();
				ServletWrapper tps=(ServletWrapper)servlet;
				tps.setServlet((Servlet)tuple[2]);
				}
		}
	}
	
	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		_log.info("tearing down");
		_server.stop();
		super.tearDown();
	}

	/**
	 * Constructor for TestServlet.
	 * 
	 * @param arg0
	 */
	public TestServlet(String arg0) {
		super(arg0);
	}
}