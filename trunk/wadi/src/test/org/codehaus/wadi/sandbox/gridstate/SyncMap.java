package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.Map;
import java.util.WeakHashMap;

import org.codehaus.wadi.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;

public class SyncMap {

	protected Map _map=new WeakHashMap(); // assocs removed as keys fall out of use... - good idea ?
	
	public Sync acquire(Serializable key) {
		Sync sync=null;
		synchronized (_map) {
			sync=(Sync)_map.get(key);
			if (sync==null)
				_map.put(key, sync=new Mutex());
		}
		Utils.safeAcquire(sync);
		return sync;
	}

}
