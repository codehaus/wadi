/*
 * Created on Feb 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface Location extends Motable {
	
	public void proxy(ServletRequest req, ServletResponse res);

}
