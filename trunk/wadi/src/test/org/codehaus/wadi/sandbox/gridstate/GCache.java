package org.codehaus.wadi.sandbox.gridstate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheEntry;
import javax.cache.CacheException;
import javax.cache.CacheListener;
import javax.cache.CacheStatistics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.Sync;


/**
 * Geronimo is going to need a standard API for lookup of sessions across the Cluster.
 * JCache is the obvious choice.
 * This will allow the plugging of either e.g. GCache (WADI), Tangosol's Coherence or IBMs solution without changing of Geronimo code.
 * In fact, this will allow WADI to sit on top of any of these three.
 * 
 * GCache is a JCache compatible interface onto DIndex - WADI's own distributed index, which fulfills
 * WADI's requirements for this lookup...
 * 
 * @author jules
 *
 */
public class GCache implements Cache, ProtocolConfig {
	
	protected final Log _log=LogFactory.getLog(getClass().getName());
	
	protected final Protocol _protocol;
	protected final BucketMapper _mapper;
	protected final Map _map=new HashMap();
	protected final SyncMap _boSyncs=new SyncMap("BO");
	protected final SyncMap _soSyncs=new SyncMap("PO/SO");
	
	
	public GCache(Protocol protocol, BucketMapper mapper) {
		(_protocol=protocol).init(this);
		_mapper=mapper;
	}
	
	/*
	 * second pass
	 */
	public boolean containsKey(Object key) {
		Sync sync=null;
		try {
			sync=_soSyncs.acquire(key);
			synchronized (_map) {
				return _map.containsKey(key);
			}
		} finally {
			sync.release();
		}
	}
	
	/*
	 * third pass
	 */
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}
	
	/*
	 * third pass
	 */
	public Set entrySet() {
		throw new UnsupportedOperationException();
	}
	
	/*
	 * second pass
	 */
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}
	
	/*
	 * second pass
	 */
	public Set keySet() {
		throw new UnsupportedOperationException();
	}
	
	/*
	 * first pass
	 */
	public void putAll(Map t) {
		// TODO Auto-generated method stub
	}
	
	/*
	 * first pass
	 */
	public int size() {
		return getCacheStatistics().getObjectCount();
	}
	
	/*
	 * third pass
	 */
	public Collection values() {
		throw new UnsupportedOperationException();
	}
	
	/*
	 * first pass
	 */
	public Object get(Object key) {
		return _protocol.get(key);
	}
	
	/*
	 * first/second pass
	 */
	public Map getAll(Collection keys) throws CacheException {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	 * second pass ?
	 */
	public void load(Object key) throws CacheException {
		throw new UnsupportedOperationException();
	}
	
	/*
	 * second pass ?
	 */
	public void loadAll(Collection keys) throws CacheException {
		throw new UnsupportedOperationException();
	}
	
	/*
	 * first pass ?
	 */
	public Object peek(Object key) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	 * first pass ?
	 */
	public Object put(Object key, Object value) {
		return put(key, value, true, true);
	}
	
	// for WADI
	public boolean putFirst(Object key, Object value) {
		return ((Boolean)put(key, value, false, true)).booleanValue();
	}
	
	// for WADI
	protected Object put(Object key, Object value, boolean overwrite, boolean returnOldValue) {
		return _protocol.put(key, value, overwrite, returnOldValue);
	}
	
	/*
	 * first pass ?
	 * interesting - perhaps this is how we make location accessible
	 */
	public CacheEntry getCacheEntry(Object key) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	 * not sure ?
	 */
	public CacheStatistics getCacheStatistics() {
		// TODO Auto-generated method stub
		return null;
		
		// needs to return :
		// objectCount
		// hits
		// misses
		
		// we will do best effort on all of these
		// they can be included in each node's distributed state and aggregated on demand
	}
	
	/*
	 * first pass
	 */
	public Object remove(Object key) {
		return _protocol.remove(key, true);
	}
	
	public Object remove(Object key, boolean returnOldValue) {
		return _protocol.remove(key, returnOldValue);
	}
	
	/*
	 * third pass
	 */
	public void clear() {
		throw new UnsupportedOperationException();
	}
	
	/*
	 * first pass ?
	 */
	public void evict() {
		// TODO Auto-generated method stub
	}
	
	/*
	 * second/third pass
	 */
	public void addListener(CacheListener listener) {
		throw new UnsupportedOperationException();
	}
	
	/*
	 * second/third pass
	 */
	public void removeListener(CacheListener listener) {
		throw new UnsupportedOperationException();
	}
	
	// Proprietary
	
	public Bucket[] getBuckets() {
		return _protocol.getBuckets();
	}
	
	// for testing...
	public Map getMap() {
		return _map;
	}
	
	public BucketMapper getBucketMapper() {
		return _mapper;
	}
	
	public SyncMap getBOSyncs() {
		return _boSyncs;
	}
	
	public SyncMap getSOSyncs() {
		return _soSyncs;
	}
	
	public BucketConfig getBucketConfig() {
		return (BucketConfig)_protocol;
	}
	
	public Protocol getProtocol() {
		return _protocol;
	}
    
	public void start() throws Exception {
    	_protocol.start();
    }
    
    public void stop() throws Exception {
    	_protocol.stop();
    }

}
