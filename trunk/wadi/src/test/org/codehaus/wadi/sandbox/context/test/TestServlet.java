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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletHttpContext;
//import org.mortbay.jetty.servlet.WebApplicationContext;

import junit.framework.TestCase;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class TestServlet extends TestCase {
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
	public synchronized void add(String name, String context, String path, Object component) {
		Object[] tuple=new Object[]{name, path, component};
		List components=(List)_map.get(context);
		if (components==null)
			components=new ArrayList();
		components.add(tuple);
		_map.put(context, components);
		}

	public void start(String host, int port)
	throws Exception
	{
		_listener.setHost(host);
		_listener.setPort(port);
		_server.addListener(_listener);

		Map contexts=new HashMap();

		// walk through entries first time setting up and starting server
		for (Iterator i=_map.entrySet().iterator(); i.hasNext();)
		{
			Map.Entry e=(Map.Entry)i.next();
			String context=(String)e.getKey();
			List components=(List)e.getValue();

			ServletHttpContext c=(ServletHttpContext)_server.getContext(context);
//			WebApplicationContext wac=(WebApplicationContext)contexts.get(context);
//			if (wac==null)
//				contexts.put(context, new WebApplicationContext());



			for (Iterator j=components.iterator(); j.hasNext();){
				Object[] tuple=(Object[])j.next();
				c.addServlet((String)tuple[0], (String)tuple[1], ServletInstance.class.getName());
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
				ServletInstance tps=(ServletInstance)servlet;
				tps.setInstance((Servlet)tuple[2]);
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
