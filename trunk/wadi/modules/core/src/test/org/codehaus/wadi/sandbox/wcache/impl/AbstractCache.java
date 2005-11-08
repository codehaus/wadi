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
import org.codehaus.wadi.sandbox.wcache.Cache;
import org.codehaus.wadi.sandbox.wcache.RequestProcessor;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class AbstractCache implements Cache {
	protected Log _log = LogFactory.getLog(getClass());

	protected Evicter _evicter;
	protected Cache _subcache;

	public AbstractCache(Evicter evicter, Cache subcache) {
		_evicter=evicter;
		_subcache=subcache;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#put(java.lang.String, org.codehaus.wadi.test.cache.RequestProcessor)
	 */
	public abstract RequestProcessor put(String key, RequestProcessor val);

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#get(java.lang.String)
	 */
	public RequestProcessor get(String key) {
		RequestProcessor val=peek(key);
		if (val==null)
			val=promote(key, _subcache);
		return val;
	}

	// TODO - optimise and further abstract so transactional caches can do their things etc..
	protected RequestProcessor promote(String key, Cache subcache) {
		RequestProcessor val=null;
		key=key.intern(); // all threads will now have same ref for key
		synchronized (key) {
			// we may have been waiting a while - check load() has not already occurred..
			if ((val=peek(key))==null)
			{
				// if val is not bound no load() has occurred - since an unsuccessful load
				// will return some form of RP for a dead session...
				val=subcache.get(key);
				put(key, val);
				_subcache.remove(key);
				_log.info("promoted: "+key+ " from "+subcache);
			}
		}
		return val;
	}

//	protected RequestProcessor demote(String key, RequestProcessor val, Cache subcache) {
//		remove(key);
//		subcache.put(key, val);
//		return val;
//	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#peek(java.lang.String)
	 */
	public abstract RequestProcessor peek(String key);

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#remove(java.lang.String)
	 */
	public abstract RequestProcessor remove(String id);

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#evict()
	 */
	public abstract void evict();

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#isOffNode()
	 */
	public abstract boolean isOffNode();
}
