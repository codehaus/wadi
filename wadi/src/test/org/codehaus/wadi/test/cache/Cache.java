/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.test.cache;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface Cache {
	public RequestProcessor put(String key, RequestProcessor rp);
	public RequestProcessor get(String id);
	public RequestProcessor peek(String id);
	public RequestProcessor remove(String id);
	public void evict();
	public boolean isOffNode();
	
	interface Joiner {
		RequestProcessor load(String key);
		void store(String key, RequestProcessor val);
	}
	
	interface Evicter {
		boolean evict(String key, RequestProcessor val);
	}
}
