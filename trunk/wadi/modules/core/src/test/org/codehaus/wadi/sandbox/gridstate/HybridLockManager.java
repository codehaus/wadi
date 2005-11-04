package org.codehaus.wadi.sandbox.gridstate;

import org.codehaus.wadi.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * A LockManager which allows you to store the lock that corresponds to an Object in that Object.
 * An Adaptor is used to extract the lock from the Object.
 * If the Object is null, the LockManager can either allocate its own lock (which will hang around for as long as it is used),
 * or just return null.
 * 
 * @author jules
 *
 */
public class HybridLockManager extends SmartLockManager {

	interface Adaptor {
		Sync adapt(Object value);
	}
	
	protected final Adaptor _adaptor;
	protected final boolean _always; 
	
	public HybridLockManager(String name, Adaptor adaptor, boolean always) { // TODO - NEEDS TESTING !!
		super(name);
		_adaptor=adaptor;
		_always=always;
	}
	
	public Sync acquire(Object key) {
		return acquire(key, null);
	}
	
	public Sync acquire(Object key, Object value) {
		Sync sync;
		if (value==null) {
			if (_always) {
				return super.acquire(key);
			} else {
				return null;
			}
		} else {
			sync=_adaptor.adapt(value);
			Utils.safeAcquire(sync);
			return sync;
		}
	}

}
