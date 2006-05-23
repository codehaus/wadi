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
package org.codehaus.wadi.gridstate.impl;

import org.codehaus.wadi.impl.SmartLockManager;
import org.codehaus.wadi.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * A LockManager which allows you to store the lock that corresponds to an Object in that Object.
 * An Adaptor is used to extract the lock from the Object.
 * If the Object is null, the LockManager can either allocate its own lock (which will hang around for as long as it is used),
 * or just return null.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
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
