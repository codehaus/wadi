/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.wcache.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.wcache.RequestProcessor;
import org.codehaus.wadi.sandbox.wcache.Cache.Evicter;

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
		if (tmp) {
            if (_log.isInfoEnabled()) _log.info("evicting due to total inactivity: " + key);
        }
		return tmp;
	}

}