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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheEntry;
import javax.cache.CacheException;
import javax.cache.CacheListener;
import javax.cache.CacheStatistics;
import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.gridstate.DispatcherConfig;
import org.codehaus.wadi.gridstate.LockManager;
import org.codehaus.wadi.gridstate.PartitionConfig;
import org.codehaus.wadi.gridstate.PartitionManager;
import org.codehaus.wadi.gridstate.PartitionManagerConfig;
import org.codehaus.wadi.gridstate.StateManager;
import org.codehaus.wadi.gridstate.StateManagerConfig;

import EDU.oswego.cs.dl.util.concurrent.Sync;

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
public class GCache implements Cache, DispatcherConfig, StateManagerConfig, PartitionManagerConfig {

	protected final Log _log=LogFactory.getLog(getClass().getName());

	protected final Dispatcher _dispatcher;
	protected final PartitionManager _partitionManager;
	protected final StateManager _stateManager;
	protected final Map _map=new HashMap();
	protected final LockManager _pmSyncs=new StupidLockManager("PM");
	protected final LockManager _smSyncs=new StupidLockManager("IM/SM");

	// interactional state - ThreadLocal

	protected ThreadLocal _threadLocks=new ThreadLocal() {
		public Object initialValue() {
			return new HashMap();
		}
	};

	// release the whole LockSet - the end of an 'interaction'
	public void release() {
		// Map is ThreadLocal - so no need to synchronise...
		for (Iterator i=((Map)_threadLocks.get()).entrySet().iterator(); i.hasNext(); ) {
			Entry entry=(Entry)i.next();
			Object key=entry.getKey();
			Sync sync=(Sync)entry.getValue();
			sync.release();
			i.remove();
            if ( _log.isInfoEnabled() ) {

                _log.info("released: " + key);
            }
		}
	}

	// add a lock to the LockSet/interaction
	protected void addLock(Object key, Sync newSync) {
		Map locks=(Map)_threadLocks.get();
		Sync oldSync=(Sync)locks.get(key);

        if ( _log.isInfoEnabled() ) {

            _log.info("adding: " + key);
        }
		if (oldSync==null) {
			locks.put(key, newSync);
		} else {
            if ( _log.isWarnEnabled() ) {

                _log.warn("NYI...");
            }
		}
	}

	public GCache(Dispatcher dispatcher, PartitionManager partitionManager, StateManager stateManager) throws Exception {
		_dispatcher=dispatcher;
		_dispatcher.init(this);
		_partitionManager=partitionManager;
		_stateManager=stateManager;
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
		return _stateManager.get(key);
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
	public Object put(Object key, Object value, boolean overwrite, boolean returnOldValue) {
		return _stateManager.put(key, value, overwrite, returnOldValue);
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
		return _stateManager.remove(key, true);
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
		return _stateManager.remove(key, returnOldValue);
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

	public PartitionFacade[] getPartitions() {
		return _partitionManager.getPartitions();
	}

	// for testing...
	public Map getMap() {
		return _map;
	}

	public LockManager getPMSyncs() {
		return _pmSyncs;
	}

	public LockManager getSMSyncs() {
		return _smSyncs;
	}

	public PartitionConfig getPartitionConfig() {
		return (PartitionConfig)_partitionManager;
	}

	public PartitionManager getPartitionManager() {
		return _partitionManager;
	}

	public StateManager getStateManager() {
		return _stateManager;
	}

	public void start() throws Exception {
    	_partitionManager.start();
    	_stateManager.start();
    }

    public void stop() throws Exception {
    	_stateManager.stop();
    	_partitionManager.stop();
    }

	public PartitionFacade getPartition(Object key) {
		return _partitionManager.getPartition(key);
	}
	
	// PartitionManagerConfig
	
	public Destination getLocalDestination() {
		return _dispatcher.getLocalDestination();
	}

	// LifeCycle
	
	public void init() throws Exception {
		_partitionManager.init(this);
		_stateManager.init(this);
	}

	// DispatcherConfig API
	
	public String getContextPath() {
		return "/";
	}
	
}
