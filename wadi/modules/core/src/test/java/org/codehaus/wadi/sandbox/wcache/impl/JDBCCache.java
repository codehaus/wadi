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

import org.codehaus.wadi.sandbox.wcache.Cache;
import org.codehaus.wadi.sandbox.wcache.RequestProcessor;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
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
