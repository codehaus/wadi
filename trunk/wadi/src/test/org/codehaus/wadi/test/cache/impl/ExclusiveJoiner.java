/*
 * Created on Feb 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.test.cache.impl;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.test.cache.RequestProcessor;
import org.codehaus.wadi.test.cache.Cache.Joiner;

/**
 * @author jules
 *
 * Ensure that load/store operations on a single key between tiers are exclusive.
 * 
 * Hmmm... - This needs more thought - TODO
 */
public class ExclusiveJoiner implements Joiner {

	protected final Joiner _joiner;
	protected final Map    _locks=new HashMap();
	
	class Lock {
		int _count=0;
	}
	
	public ExclusiveJoiner(Joiner joiner){_joiner=joiner;}
	
	public RequestProcessor load(String key) {

		Lock lock=null;
		synchronized (_locks) {
			if ((lock=(Lock)_locks.get(key))==null)
				_locks.put(key, (lock=new Lock()));
			lock._count++;
		}
		
		RequestProcessor val=null;
		
		synchronized (lock) {
			val=_joiner.load(key);
		}
		
		if (--lock._count==0)
		{
			synchronized (_locks)
			{
				if (lock._count==0)// TODO - is this OK - more thought...
					_locks.remove(key);
			}
		}
		
		return val;
	}

	public void store(String key, RequestProcessor val) {
		// TODO - per key locking here - do we need it - how would it work ?
		_joiner.store(key, val);
		}
}
