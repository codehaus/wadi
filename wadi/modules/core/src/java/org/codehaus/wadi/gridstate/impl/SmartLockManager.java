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

import java.util.HashMap;
import java.util.Map;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.gridstate.LockManager;
import org.codehaus.wadi.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;

// FIXME - DOES NOT WORK :-(

/**
 * Creates, reuses and destroys, on-the-fly, a lock for a given Key.
 *
 * @author jules
 *
 */
public class SmartLockManager implements LockManager {

	/**
	 * Like a ManagedConnection in a JCA ConnectionPool... - but simpler :-)
	 *
	 */
	class ManagedSync implements Sync {

		protected final Object _key;
		protected final Sync _sync;
		protected int _count;

		ManagedSync(Object key) {
			_key=key;
			_sync=new Mutex();
			_count=0;
		}

		public void acquire() throws InterruptedException {
			inc();
			_sync.acquire();
		}

		public boolean attempt(long msecs) throws InterruptedException {
			inc();
			if(_sync.attempt(msecs)) {
				return true;
			} else {
				dec();
				return false;
			}

		}

		public void release() {
			_sync.release();
			dec();
		}

		protected void inc() {
			_count++;
			//_log.info("inc: "+_key+" ->"+_count+" : "+Thread.currentThread().getName());
		}

		protected void dec() {
			synchronized (_syncs) {
				if (--_count==0) {
					_syncs.remove(_key);
					//_log.info("dec: "+_key+" ->"+_count+" : "+Thread.currentThread().getName());
					//_log.info("destroyed: "+_key+" : "+Thread.currentThread().getName());
				} else {
					//_log.info("dec: "+_key+" ->"+_count+" : "+Thread.currentThread().getName());
				}
			}
		}
	}

	protected final String _name;
	//protected final Log _log;
	protected final Map _syncs;

	public SmartLockManager(String name) {
		_name=name;
		//_log=LogFactory.getLog(getClass().getName()+"#"+hashCode());
		_syncs=new HashMap();
	}

	public Sync acquire(Object key) {
		Sync sync;
		synchronized (_syncs) {
			if ((sync=(Sync)_syncs.get(key))==null) {
				_syncs.put(key, (sync=new ManagedSync(key)));
				//_log.info("created: "+key+" : "+Thread.currentThread().getName());
			}
			Utils.safeAcquire(sync);
		}

		return sync;
	}

	public Sync acquire(Object key, Object value) {
		return acquire(key);
	}

}
