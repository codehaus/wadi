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

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface Promoter {

	void promoteAndContextualise(ServletRequest req, ServletResponse res, FilterChain chain, String id, Context context, Sync overlap) throws IOException, ServletException;
	
	Context nextContext();
}
