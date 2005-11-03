package org.codehaus.wadi.sandbox.gridstate;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * This should actually be a LockManager giving out SyncWrappers which contain a Sync and a counter, hooked into a backing Map.
 * Syncs that do not exist in the Map are created on demand.
 * When the counter hits '0', the Sync is removed from the Map.
 * Also needs to be integrated with the rest of WADI's locking policies...
 * 
 * @author jules
 *
 */
public class LockManager {

	protected static final Log _log=LogFactory.getLog(LockManager.class);
	
	protected String _prefix;
	
	public LockManager(String prefix) {
		_prefix=prefix;
	}
	
	protected Map _map=new HashMap(); // was a WeakHashMap, assocs removed as keys fall out of use... - good idea ?
	
	public Sync acquire(Object key) {
		Sync sync=null;
		synchronized (_map) {
			sync=(Sync)_map.get(key);
			if (sync==null) {
					_map.put(key, sync=new Mutex());
					_log.trace("["+_prefix+"] created sync: "+key+" - "+this+" - "+sync);
			}
		}
		_log.trace("["+_prefix+"] trying to acquire sync for: "+key+" - "+this+" - "+sync);
		Utils.safeAcquire(sync);
		_log.trace("["+_prefix+"] sync acquired for: "+key+" - "+this+" - "+sync);
		return sync;
	}

}
