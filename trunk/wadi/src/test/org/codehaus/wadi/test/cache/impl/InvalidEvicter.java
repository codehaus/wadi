/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.test.cache.impl;

import org.codehaus.wadi.test.cache.RequestProcessor;
import org.codehaus.wadi.test.cache.Cache.Evicter;

/**
 * @author jules
 *
 * Choose to evict given entry if it is 'invalid'
 */
public class InvalidEvicter implements Evicter {

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache.Evicter#evict(java.lang.String, org.codehaus.wadi.test.cache.RequestProcessor)
	 */
	public boolean evict(String key, RequestProcessor val) {
		return val.getTimeToLive()<=0;// also need to test for invalidity - or id this the same thing ?
	}

}
