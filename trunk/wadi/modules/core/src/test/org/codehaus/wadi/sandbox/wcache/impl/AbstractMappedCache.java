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

import java.util.Iterator;
import java.util.Map;

import org.codehaus.wadi.sandbox.wcache.Cache;
import org.codehaus.wadi.sandbox.wcache.RequestProcessor;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class AbstractMappedCache extends AbstractCache {

	protected final Map _map;

	public AbstractMappedCache(Map map, Evicter evicter, Cache subcache) {
		super(evicter, subcache);
		_map=map;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.test.cache.Cache#evict()
	 */
	public void evict() {
		for (Iterator i=_map.entrySet().iterator(); i.hasNext();) {
			Map.Entry e=(Map.Entry)i.next();
			String key=(String)e.getKey();
			RequestProcessor val=(RequestProcessor)e.getValue();
			// TODO - we need an exclusive lock on entry before we try to evict (demote) it...
			if (_evicter.evict(key, val)) {
				_subcache.put(key, val);
				i.remove();
                if (_log.isInfoEnabled()) _log.info("demoted: " + key + " to " + _subcache);
			}
			// TODO - release exclusive lock here...
		}
	}

	public String toString() {
		return "<"+getClass().getName()+":"+_map.size()+">";
	}
}
