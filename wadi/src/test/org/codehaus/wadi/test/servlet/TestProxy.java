/*
 * Created on Feb 11, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.test.servlet;

import java.io.IOException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * @author jules
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class TestProxy extends TestServlet{

	public class ProxyServlet extends HttpServlet {
		public void service(ServletRequest req, ServletResponse res)
				throws ServletException, IOException {
			new HttpProxy().proxy(req, res, new URL("http://localhost:8080/test/admin"));
		}
	}
	
	public class ProxyFilter implements Filter {
		public void init(FilterConfig config){
		}
		
		public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
				throws ServletException, IOException {
			new HttpProxy().proxy(req, res, new URL("http://localhost:8080/test/admin"));
		}
		
		public void destroy(){
		}
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		_log.info("setting up");
		add("Proxy", "/test", "/proxy", new ProxyFilter());
		add("Admin", "/test", "/admin", new org.mortbay.servlet.AdminServlet());
		start("localhost", 8080);
		}

	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		_log.info("tearing down");
		super.tearDown();
	}

	/**
	 * Constructor for TestProxy.
	 * 
	 * @param arg0
	 */
	public TestProxy(String arg0) {
		super(arg0);
	}

	public void testGET() throws Exception {
		HttpMethod get=new GetMethod("http://localhost:8080");
		get.setPath("/test/proxy");
		HttpClient client=new HttpClient();
		client.executeMethod(get);
		String proxied=get.getResponseBodyAsString();
		_log.info(proxied);
		get.recycle();
		get.setPath("/test/admin");
		client.executeMethod(get);
		String direct=get.getResponseBodyAsString();
		_log.info(direct);
		// assertTrue(direct.equals(proxied));
		//		Thread.sleep(60*1000);
	}

}