package org.codehaus.wadi.sandbox.gridstate;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Creates and reuses, on-the-fly, a lock for a given Key, but will never destroy the lock - temporary solution...
 * 
 * @author jules
 *
 */
public class StupidLockManager implements LockManager {

	protected static final Log _log=LogFactory.getLog(StupidLockManager.class);
	
	protected String _prefix;
	
	public StupidLockManager(String prefix) {
		_prefix=prefix;
	}
	
	protected Map _syncs=new HashMap(); // was a WeakHashMap, assocs removed as keys fall out of use... - good idea ?
	
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.gridstate.LockManagerAPI#acquire(java.lang.Object)
	 */
	public Sync acquire(Object key) {
		return acquire(key, null);
	}

	public Sync acquire(Object key, Object value) {
		// value not used - locks are always held externally...
		Sync sync=null;
		synchronized (_syncs) {
			if ((sync=(Sync)_syncs.get(key))==null) {
					_syncs.put(key, (sync=new Mutex()));
					_log.trace("["+_prefix+"] created sync: "+key+" - "+this+" - "+sync);
			}
		}
		_log.trace("["+_prefix+"] trying to acquire sync for: "+key+" - "+this+" - "+sync);
		Utils.safeAcquire(sync);
		_log.trace("["+_prefix+"] sync acquired for: "+key+" - "+this+" - "+sync);
		return sync;
	}

}
