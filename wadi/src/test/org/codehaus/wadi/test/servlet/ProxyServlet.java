/*
 * Created on Feb 24, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.test.servlet;

import java.io.IOException;
import java.net.URL;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHttpContext;
import org.mortbay.util.InetAddrPort;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ProxyServlet implements Servlet {
	protected final Log _log = LogFactory.getLog(getClass());

	protected ServletConfig _config;
	protected ServletContext _context;
	
	protected HttpProxy _proxy=new HttpProxy();
	
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
		String uri=hreq.getRequestURI();
		String qs=hreq.getQueryString();
		if (qs!=null) {
			uri=new StringBuffer(uri).append("?").append(qs).toString();
		}
		assert req.getScheme().equalsIgnoreCase("http");
		URL url=new URL(req.getScheme(), req.getServerName(), req.getServerPort(), uri);
		_proxy.proxy(req, res, url);
	}
	
	public String getServletInfo() {
		return "Proxy Servlet";
	}
	
	public void destroy() {
	}
	
	public static void main(String[] args) throws Exception {
		Server server=new Server();
		SocketListener listener=new SocketListener(new InetAddrPort(8080));
		listener.setMinThreads(1000);
		listener.setMaxThreads(2000);
		server.addListener(listener);
		ServletHttpContext context=(ServletHttpContext)server.getContext("/");
		context.addServlet("Proxy", "/", ProxyServlet.class.getName());
		server.start();		
	}
}
