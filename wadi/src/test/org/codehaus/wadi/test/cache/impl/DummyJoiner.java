/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.test.cache.impl;

import org.codehaus.wadi.test.cache.RequestProcessor;
import org.codehaus.wadi.test.cache.Cache.Joiner;

/**
 * @author jules
 *
 * The end of the road - a Joiner which will promote no content and will throw away any content demoted to it.
 */
public class DummyJoiner implements Joiner {

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache.Loader#load(java.lang.String)
	 */
	public RequestProcessor load(String key) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void store(String key, RequestProcessor val){}
}
