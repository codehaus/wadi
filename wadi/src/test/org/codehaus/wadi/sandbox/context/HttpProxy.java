/*
 * Created on Mar 1, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context;

import java.net.URL;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface HttpProxy {
	// move this into ProxyServlet...
	public abstract void proxy(ServletRequest req, ServletResponse res, URL url);
}