/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.test.cache.impl;

import org.codehaus.wadi.test.cache.Cache;
import org.codehaus.wadi.test.cache.RequestProcessor;

/**
 * @author jules
 *
 * Promotion from this tier should result in content being loaded from JDBC
 * Demotion to this tier should result in content being store via JDBC
 */
public class JDBCCache implements Cache {

	protected final Evicter _evicter;
	protected final Cache _subcache;
	
	public JDBCCache(Evicter evicter, Cache subcache) {
		_evicter=evicter;
		_subcache=subcache;
	}
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#put(java.lang.String, org.codehaus.wadi.test.cache.RequestProcessor)
	 */
	public RequestProcessor put(String key, RequestProcessor rp) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#get(java.lang.String)
	 */
	public RequestProcessor get(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#peek(java.lang.String)
	 */
	public RequestProcessor peek(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#remove(java.lang.String)
	 */
	public RequestProcessor remove(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#evict()
	 */
	public void evict() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#isOffNode()
	 */
	public boolean isOffNode() {
		// TODO Auto-generated method stub
		return true;
	}

}
