/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.wcache;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.codehaus.wadi.SerializableContent;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface RequestProcessor extends SerializableContent {
	void process(ServletRequest req, ServletResponse res, FilterChain chain);
	
	// temporary
	public long getTimeToLive();
	public void setTimeToLive(long ttl);
	
	public int getMaxInactiveInterval();
	public void setMaxInactiveInterval(int mii);
}
