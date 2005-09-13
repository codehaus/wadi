package org.codehaus.wadi.test;


import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.catalina.Connector;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.core.FilterConfigHelper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.startup.Embedded;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.tomcat50.Valve;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class TomcatNode implements Node {
	protected final Log _log = LogFactory.getLog(getClass());
	protected final Embedded _server;
	protected final Engine _engine;
	protected final Host _host;
	protected final Connector _connector;
	protected final StandardContext _context;
	protected StandardWrapper _wrapper;
	protected final Filter _filter;
	protected final Servlet _servlet;

	/**
	 *
	 */
	public TomcatNode(String name, String host, int port, String context, String webApp, Filter filter, Servlet servlet) {
		super();
		// TODO Auto-generated method stub
		String home="/usr/local/java/jakarta-tomcat-5.0.28";
		System.setProperty("catalina.home", home);
		System.setProperty("catalina.base", home);
		_server=new Embedded();

	    // Context
	    _context=(StandardContext)_server.createContext(context, webApp);
//	    _context.setSaveConfig(false);
//	    _context.setOverride(true);
//	    _context.setTldValidation(false);
	    _context.addValve(new Valve(Pattern.compile("127\\.0\\.0\\.1|192\\.168\\.0\\.\\d{1,3}")));
	    //_context.start();

	    // Host
	    _host=_server.createHost("localhost", "/home/jules/workspace/wadi/webapps");
	    _host.addChild(_context);
	    // Engine
	    _engine=_server.createEngine();
	    _engine.setDefaultHost("localhost");
	    _engine.addChild(_host);

	    // Server
	    _server.addEngine(_engine);

	    // Assemble and install a default HTTP connector
	    _connector=_server.createConnector((String)null, port, false);
	    _connector.setRedirectPort(0);
	    _server.addConnector(_connector);

	    _filter=filter;
	    _servlet=servlet;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.test.Node#getFilter()
	 */
	public Filter getFilter() {
		return FilterConfigHelper.getFilter(_context.findFilterConfig("Filter"));
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.test.Node#getServlet()
	 */
	public Servlet getServlet() {
		Servlet servlet=null;
		try {
			servlet=_wrapper.allocate();
		} catch (ServletException e) {
			_log.error(e);
		}
		return servlet;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.test.Node#start()
	 */
	public void start() throws Exception {
		//_context.start();
	    _server.start();

	    _wrapper=(StandardWrapper)_context.findChild("Servlet");
		_wrapper.setMaxInstances(1);

		((FilterInstance)getFilter()).setInstance(_filter);
		((ServletInstance)getServlet()).setInstance(_servlet);	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.test.Node#stop()
	 */
	public void stop() throws Exception {
		// TODO Auto-generated method stub
		_server.stop();
	}

	// Securable - NYI
	public boolean getSecure(){return _connector.getSecure();}
	public void setSecure(boolean secure){_connector.setSecure(secure);}
}
