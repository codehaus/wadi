/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.wcache.impl;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.codehaus.wadi.sandbox.wcache.RequestProcessor;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class StatelessRequestProcessor implements RequestProcessor {

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.RequestProcessor#process(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	public void process(ServletRequest req, ServletResponse res,
			FilterChain chain) {
		// TODO Auto-generated method stub

	}
	public long getTimeToLive() {return 0;}//TODO
	public void setTimeToLive(long ttl) {}//TODO

	public int getMaxInactiveInterval() {return 0;}//TODO
	public void setMaxInactiveInterval(int mii) {} //TODO
	
	public void readContent(ObjectInput is)
	    throws IOException, ClassNotFoundException {
		// TODO
	  }

	public void writeContent(ObjectOutput os)
	    throws IOException, ClassNotFoundException {
		// TODO
	  }
	}
