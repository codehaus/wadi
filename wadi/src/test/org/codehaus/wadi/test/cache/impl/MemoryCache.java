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

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

/**
 * @author jules
 *
 * A Map of content in local memory. Exclusive access to the map is assumed.
 */
public class MemoryCache extends AbstractMappedCache {
	protected static final Log _log = LogFactory.getLog(MemoryCache.class);

	public MemoryCache(Joiner joiner, Evicter evicter) {
		super(joiner, evicter, new ConcurrentReaderHashMap());
	}
	
	public RequestProcessor put(String key, RequestProcessor val){return (RequestProcessor)_map.put(key, val);}
	public RequestProcessor peek(String key) {return (RequestProcessor)_map.get(key);}
	public RequestProcessor remove(String key) {return (RequestProcessor)_map.remove(key);}

	public boolean isOffNode() {return false;}
}
