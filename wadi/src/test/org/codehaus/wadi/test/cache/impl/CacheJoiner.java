/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.test.cache.impl;

import org.codehaus.wadi.test.cache.Cache;
import org.codehaus.wadi.test.cache.RequestProcessor;
import org.codehaus.wadi.test.cache.Cache.Joiner;

/**
 * @author jules
 *
 * A Joiner that is backed with another Cache
 */
public class CacheJoiner implements Joiner {

	protected final Cache _cache;

	public CacheJoiner(Cache cache) {_cache=cache;}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache.Loader#load(java.lang.String)
	 */
	public RequestProcessor load(String key) {
		return _cache.get(key); // what if we only want a peek() ?
	}
	
	public void store(String key, RequestProcessor val) {
		_cache.put(key, val);
	}
}
