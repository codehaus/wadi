package org.codehaus.wadi.sandbox.gridstate;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.Utils;
import org.jgroups.Address;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;

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
	
	public Address getAddress() {
		return _config.getLocalAddress();
	}
	
	public Destination getDestination() {
		return _config.getLocalDestination();
	}
	
	public Location getLocation(Object key) {
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
	
}
