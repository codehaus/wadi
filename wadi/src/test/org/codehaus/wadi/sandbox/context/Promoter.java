/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface Promoter {
		
	Context nextContext();
	
	boolean prepare(String id, Context context);
	void commit(String id, Context context);
	void rollback(String id, Context context);
	
	void contextualise(ServletRequest req, ServletResponse res, FilterChain chain, String id, Context context) throws IOException, ServletException;
}
