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

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.apache.catalina.Connector;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.startup.Embedded;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class TomcatNode implements Node {
	protected final Embedded _server;
	protected final Engine _engine;
	protected final Host _host;
	protected final Connector _connector;
	protected final Context _context;

	/**
	 * 
	 */
	public TomcatNode(String name, String host, int port, String context, String pathSpec, Filter filter, Servlet servlet) throws UnknownHostException {
		super();
		// TODO Auto-generated method stub
		String home="/usr/local/java/jakarta-tomcat-5.0.28";
		System.setProperty("catalina.home", home);
		_server=new Embedded();
//	    _server.setDebug(0);
//	    _server.setLogger(new SystemOutLogger());
	    _engine=_server.createEngine();
	    _engine.setDefaultHost("localhost");
	    // Create a default virtual host
	    _host = _server.createHost("localhost", home+"/webapps");
	    _engine.addChild(_host);

//	    // Create the ROOT context
//	    Context context = _tc.createContext("", home+"/webapps/ROOT");
//	    host.addChild(context);
	    _context=_server.createContext(context, home+"/webapps/ROOT");
	    
	    // Install the assembled container hierarchy
	    _server.addEngine(_engine);

	    // Assemble and install a default HTTP connector
	    _connector=_server.createConnector((String)null, 8080, false);
	    _server.addConnector(_connector);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.test.Node#getFilter()
	 */
	public Filter getFilter() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.test.Node#getServlet()
	 */
	public Servlet getServlet() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.test.Node#start()
	 */
	public void start() throws Exception {
	    // Start the tc server
	    _server.start();

	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.test.Node#stop()
	 */
	public void stop() throws Exception {
		// TODO Auto-generated method stub
		_server.stop();
	}

	// Securable - NYI
	public boolean getSecure(){return false;}
	public void setSecure(boolean secure){}
}
