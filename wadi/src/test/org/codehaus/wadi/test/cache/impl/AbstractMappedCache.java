/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.test.cache.impl;

import java.util.Iterator;
import java.util.Map;

import org.codehaus.wadi.test.cache.RequestProcessor;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class AbstractMappedCache extends AbstractCache {

	protected final Map _map;
	
	public AbstractMappedCache(Joiner joiner, Evicter evicter, Map map) {
		super(joiner, evicter);
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
				_joiner.store(key, val);
				_map.remove(key);
			}
			// TODO - release exclusive lock here...
		}
	}
}
