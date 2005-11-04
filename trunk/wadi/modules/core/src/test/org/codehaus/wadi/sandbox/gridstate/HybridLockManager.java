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
		boolean isValid(Object value);
	}
	
	protected final Adaptor _adaptor;
	protected final boolean _always; 
	
	// There are two ways we can lock :
	// 1. keep locks outside objects locked - even if we have not yet got the object in our hand we can lock it
	// 2. keep lock in object - retrieve object, lock it and then check it is still valid (may have been removed between finding object and acquiring lock)
	// This class allows us to use (2) in the case where the object sometimes does not exist at lock time...
	
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
			if (sync==null) {
				return null;
			} else {
				Utils.safeAcquire(sync);
				if (_adaptor.isValid(value)) {
					return sync;
				} else {
					sync.release();
					return null;
				}
			}
		}
	}

}
