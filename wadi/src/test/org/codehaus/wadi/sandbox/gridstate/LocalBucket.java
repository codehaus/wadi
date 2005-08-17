package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;

public class LocalBucket implements BucketInterface {
	
	protected static final Log _log=LogFactory.getLog(LocalBucket.class);
	
	protected final transient ReadWriteLock _lock;
	protected Map _map=new HashMap();

	protected BucketConfig _config;

	public LocalBucket() {
		_lock=new ReaderPreferenceReadWriteLock();
	}
	
	public void init(BucketConfig config) {
		_config=config;
	}
	
	public Destination getDestination() {
		return _config.getLocalNode().getDestination();
	}
	
	public Location getLocation(Serializable key) {
		try {
			Utils.safeAcquire(_lock.readLock());
			return (Location)_map.get(key);
		} finally {
			_lock.readLock().release();
		}
	}
	
	public ReadWriteLock getLock() {
		return _lock;
	}
	
	public Map getMap() {
		return _map;
	}
	
	//--------------------------------------------------------------------------------
	// PutAbsent Protocol
	//--------------------------------------------------------------------------------
	
	/* 
	 * If Key is not already in use, lock and associate a new Location with this Key.
	 * 
	 * @see org.codehaus.wadi.sandbox.gridstate.BucketInterface#putAbsentPrepare(java.io.Serializable, javax.jms.Destination)
	 */
	public boolean putAbsentBegin(Conversation conversation, Serializable key, Destination destination) { // consider optimisations...
		// we are expecting the key to be absent and want to maintain a lock on the map for as short a time as possible...
		
		// create a new Location
		Location oldLocation=null;
		Location newLocation=new Location(destination);
		Sync newLocationLock=newLocation.getLock().writeLock(); // exclusive because we have changed location
		// take an exclusive lock on it
		Utils.safeAcquire(newLocationLock); // to be released during commit or rollback phase
		// take an exclusive lock on the Bucket
		Sync bucketLock=_lock.writeLock(); // exclusive, because we will change structure of bucket
		try {
			Utils.safeAcquire(bucketLock);
			
			// insert new association - assuming that key is unused
			oldLocation=(Location)_map.put(key, newLocation);
			// if not - undo last action
			if (oldLocation!=null)
				_map.put(key, oldLocation);
		} finally {
			bucketLock.release();
		}
		
		if (oldLocation!=null) {
			_log.warn("key already associated - undoing insertion");
			newLocation.invalidate();
			newLocationLock.release();
			return false;
		} else {
			return true;
		}
	}

	/*
	 * Unlock the Location associated with this Key.
	 *  
	 * @see org.codehaus.wadi.sandbox.gridstate.BucketInterface#putAbsentCommit(java.io.Serializable, javax.jms.Destination)
	 */
	public void putAbsentCommit(Conversation conversation, Serializable key, Destination destination) {
		Sync bucketLock=_lock.readLock();
		Location location=null;
		try {
			Utils.safeAcquire(bucketLock);
			location=(Location)_map.get(key);
		} finally {
			bucketLock.release();
		}
		location.getLock().writeLock().release(); // exclusive, because we have changed location
	}
	
	/*
	 * Remove, invalidate and unlock the Location associated with this Key.
	 * 
	 * @see org.codehaus.wadi.sandbox.gridstate.BucketInterface#putAbsentRollback(java.io.Serializable, javax.jms.Destination)
	 */
	public void putAbsentRollback(Conversation conversation, Serializable key, Destination destination) {
		Sync bucketLock=_lock.writeLock(); // exclusive, because we will change structure of bucket
		Location location=null;
		try {
			Utils.safeAcquire(bucketLock);
			location=(Location)_map.remove(key);
		} finally {
			bucketLock.release();
		}
		location.invalidate(); // if anyone gets lock, they will see that Location is now invalid...
		location.getLock().writeLock().release(); // exclusive , because we were changing value of location
	}
	
	//--------------------------------------------------------------------------------
				
	public Destination putExists(Serializable key, Destination destination) {
		Location location;
		synchronized (_map) {
			location=(Location)_map.put(key, new Location(destination));
		}
		return location==null?null:location.getDestination();
	}

	public Serializable removeReturn(Serializable key, Map data) {
		synchronized (_map) {
			Location location=(Location)_map.remove(key);
			if (location!=null && location.getDestination()==_config.getLocalNode().getDestination()) { // i.e. Data and Bucket present in same vm...
				synchronized (data) {
					return (Serializable)data.remove(key);
				}
			} else {
				throw new UnsupportedOperationException("NYI");
				// we will have to retrieve the data from another vm...
				// we can avoid this is the request came from the vm that currently holds the data
			}
		}
	}
	
	public void removeNoReturn(Serializable key) {
		synchronized (_map) {
			_map.remove(key);
		}
	}
	
}
