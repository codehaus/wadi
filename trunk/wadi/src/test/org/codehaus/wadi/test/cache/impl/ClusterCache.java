/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.test.cache.impl;

import java.util.Map;

import org.codehaus.wadi.test.cache.RequestProcessor;
import org.codehaus.wadi.test.cache.Cache;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;

/**
 * @author jules
 *
 * Promotion from this cache may result in some for of lazy remote reference.
 * Demotion to this cache should result in a migration off-node.
 * 
 * This tier is responsible for finding, caching and listening out for changes to the location of recently mobile content. 
 */
public class ClusterCache implements Cache {

	protected final Joiner _joiner;
	protected final Evicter _evicter;
	protected final Map _map=new ConcurrentReaderHashMap();

	// TODO add ClusterListener for migrations - feed them into internal map
	public ClusterCache(Joiner joiner, Evicter evicter){_joiner=joiner; _evicter=evicter;}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#put(java.lang.String, org.codehaus.wadi.test.cache.RequestProcessor)
	 */
	public RequestProcessor put(String key, RequestProcessor val) {
		// TODO - check for relevant waiting get()s - wake them.
		// do not raise notification, this will be done in migration code
		return (RequestProcessor)_map.put(key, val);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#get(java.lang.String)
	 */
	public RequestProcessor get(String key) {
		RequestProcessor val=peek(key);
		// TODO - send message querying location to whole cluster - waiting, with timeout, for an update to our _map;	
		
		return val==null?_joiner.load(key):val;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#peek(java.lang.String)
	 */
	public RequestProcessor peek(String key) {
		return (RequestProcessor)_map.get(key);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#remove(java.lang.String)
	 */
	public RequestProcessor remove(String key) {
		return (RequestProcessor)_map.remove(key);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#evict()
	 */
	public void evict() {
		// TODO Auto-generated method stub
		
		// two types of eviction happening here...
		
		// eviction of keys from key list
		// demotion of content to lower tiers...(if no space left in cluster)
		
		// do we need to split this cache in half - or have two evicters - or...

	}
	
	public boolean isOffNode() {return true;}
}
