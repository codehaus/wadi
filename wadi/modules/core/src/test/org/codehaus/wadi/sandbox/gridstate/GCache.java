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

/**
 * Geronimo is going to need a standard API for lookup of sessions across the Cluster.
 * JCache is the obvious choice.
 * This will allow the plugging of either e.g. GCache (WADI), Tangosol's Coherence or IPMs solution without changing of Geronimo code.
 * In fact, this will allow WADI to sit on top of any of these three.
 *
 * GCache is a JCache compatible interface onto DIndex - WADI's own distributed index, which fulfills
 * WADI's requirements for this lookup...
 *
 * @author jules
 *
 */
/**
 * @author jules
 *
 */
public class GCache implements Cache, ProtocolConfig {

	protected final Log _log=LogFactory.getLog(getClass().getName());

	protected final Protocol _protocol;
	protected final PartitionMapper _mapper;
	protected final Map _map=new HashMap();
	protected final LockManager _pmSyncs=new StupidLockManager("PM");
	protected final LockManager _smSyncs=new StupidLockManager("IM/SM");


	public GCache(Protocol protocol, PartitionMapper mapper) {
		(_protocol=protocol).init(this);
		_mapper=mapper;
	}

	/*
	 * Does the local cache contain the given key ? Does no locking, so if it replies true
	 *  and you go back to the cache for the value, it may have evaporated...Is this correct ?
	 */
	public boolean containsKey(Object key) {
		// TODO - correct ?
		synchronized (_map) {
			return _map.containsKey(key);
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
	 * Find, globally, the value associated with this key and return it.
	 */
	public Object get(Object key) {
		return _protocol.get(key);
	}

	/*
	 * first/second pass
	 */
	public Map getAll(Collection keys) throws CacheException {
		throw new UnsupportedOperationException();
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
	 * Find, locally, the value associated with this key and return it (no loaders called)
	 */
	public Object peek(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * Put, globally, this key:value association, returning any value previously globally associated with the same key.
	 */
	public Object put(Object key, Object value) {
		return put(key, value, true, true);
	}


	/**
	 * Extension: Insert, globally, the key:value association for the first time. This can be more efficient than a put(),
	 * as we can be sure that it will not have to return a value that is held remotely. Returns success if the insertion was
	 * able to occur (i.e. there was no value previously associated with this key).
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	public boolean putFirst(Object key, Object value) {
		return ((Boolean)put(key, value, false, true)).booleanValue();
	}


	/**
	 * Extension: Insert, globally, the key:value association only overwriting a previous value and returning it, if
	 * the relevant flags are passed in. If overwrite is true, returnOldValue will return any value previously associated
	 * with this key, else the insertion will occur and return Boolean.TRUE only if NO previous association exists otherwise
	 * Boolean.FALSE will be returned.
	 *
	 * @param key
	 * @param value
	 * @param overwrite
	 * @param returnOldValue
	 * @return
	 */
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
	 * Remove, globally, any current association with this key, returning the correspondong value.
	 */
	public Object remove(Object key) {
		return _protocol.remove(key, true);
	}

	/**
	 * Remove the key's association globally, returning its current value if the flag is true.
	 * If the value is held remotely and not required, it will save bandwidth to pass in a value of false.
	 *
	 * @param key
	 * @param returnOldValue
	 * @return
	 */
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

	public Partition[] getPartitions() {
		return _protocol.getPartitions();
	}

	// for testing...
	public Map getMap() {
		return _map;
	}

	public PartitionMapper getPartitionMapper() {
		return _mapper;
	}

	public LockManager getPMSyncs() {
		return _pmSyncs;
	}

	public LockManager getSMSyncs() {
		return _smSyncs;
	}

	public PartitionConfig getPartitionConfig() {
		return (PartitionConfig)_protocol;
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
