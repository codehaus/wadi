/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.test.cache.impl;

import org.codehaus.wadi.test.cache.Cache;
import org.codehaus.wadi.test.cache.RequestProcessor;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

/**
 * @author jules
 *
 * Promotion from this cache will result in the loading of content from local disc
 * Demotion to this cache will result in the storing of content onto local disc.
 * Assumptions are made about the exclusive ownership of the directory and files used.
 * Content keys will be cached in memory, values on disc.
 * 
 * This tier is intended as a disc spool for large amounts of frequently used content.
 * 
 * This could return lazy references to on disc objects... consider...
 */
public class LocalDiscCache extends AbstractMappedCache {

	public LocalDiscCache(Evicter evicter, Cache subcache) {
		super(new ConcurrentHashMap(), evicter, subcache);
	}
	
	public RequestProcessor put(String key, RequestProcessor val){return (RequestProcessor)_map.put(key, val);}
	public RequestProcessor peek(String key) {return (RequestProcessor)_map.get(key);}
	public RequestProcessor remove(String key) {return (RequestProcessor)_map.remove(key);}

	public boolean isOffNode() {return false;}
}
