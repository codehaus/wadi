/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.test.cache.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.test.cache.RequestProcessor;
import org.codehaus.wadi.test.cache.Cache.Evicter;

/**
 * @author jules
 *
 * Choose to evict given entry if it is considered 'inactive'
 */
public class InactiveEvicter implements Evicter {
	protected Log _log = LogFactory.getLog(InactiveEvicter.class);

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache.Evicter#evict(java.lang.String, org.codehaus.wadi.test.cache.RequestProcessor)
	 */
	public boolean evict(String key, RequestProcessor val) {
		boolean tmp=val.getTimeToLive()<10000;
		if (tmp) _log.info("evicting due to lack of activity: "+key);
		return tmp;
	}
}
