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
 * Choose to evict given entry if it has remained inactive for more than a than a given Max-Inactive-Interval
 */
public class MaxInactiveIntervalEvicter implements Evicter {
	protected Log _log = LogFactory.getLog(MaxInactiveIntervalEvicter.class);

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache.Evicter#evict(java.lang.String, org.codehaus.wadi.test.cache.RequestProcessor)
	 */
	public boolean evict(String key, RequestProcessor val) {
		boolean tmp=val.getTimeToLive()<=0;
		if (tmp) _log.info("evicting due to total inactivity: "+key);
		return tmp;
	}

}
