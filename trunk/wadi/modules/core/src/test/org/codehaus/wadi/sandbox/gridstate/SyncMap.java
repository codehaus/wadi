package org.codehaus.wadi.sandbox.gridstate;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;

public class SyncMap {

	protected static final Log _log=LogFactory.getLog(SyncMap.class);
	
	protected String _prefix;
	
	public SyncMap(String prefix) {
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
