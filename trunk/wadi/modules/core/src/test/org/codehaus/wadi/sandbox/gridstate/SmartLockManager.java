package org.codehaus.wadi.sandbox.gridstate;

import java.util.HashMap;
import java.util.Map;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;

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
			if (--_count==0) {
				synchronized (_syncs) {
					_syncs.remove(_key);
					//_log.info("dec: "+_key+" ->"+_count+" : "+Thread.currentThread().getName());
					//_log.info("destroyed: "+_key+" : "+Thread.currentThread().getName());
				}
			} else {
				//_log.info("dec: "+_key+" ->"+_count+" : "+Thread.currentThread().getName());
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
		}
		
		Utils.safeAcquire(sync);
		return sync;
	}
	
	public Sync acquire(Object key, Object value) {
		return acquire(key);
	}
	
}
