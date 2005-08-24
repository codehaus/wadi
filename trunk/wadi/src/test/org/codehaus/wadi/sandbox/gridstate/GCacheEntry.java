package org.codehaus.wadi.sandbox.gridstate;

import javax.cache.CacheEntry;

import org.codehaus.wadi.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;

public class GCacheEntry implements CacheEntry {

	// GCacheEntry
	
	protected ReadWriteLock _lock=new ReaderPreferenceReadWriteLock();
	
	// the read lock is used by any thread wanting to keep this entry in this JVM
	public void acquireReadLock() {
		Utils.safeAcquire(_lock.readLock());
	}
	
	public void releaseReadLock() {
		_lock.readLock().release();
	}
	
	// the write lock is used by any thread wishing to remove this entry from this jvm
	public Sync getWriteLock() {
		return _lock.writeLock();
	}
	
	protected Object _value;
	
	public GCacheEntry(Object value) {
		_value=value;
	}
	
	// CacheEntry

	public int getHits() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getLastAccessTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getLastUpdateTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getCreationTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getExpirationTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	public long getVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	public long getCost() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Object getKey() {
		throw new UnsupportedOperationException("NYI");
	}

	public Object getValue() {
		return _value;
	}

	public Object setValue(Object value) {
		throw new UnsupportedOperationException("NYI");
	}

}
