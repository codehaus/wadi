/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.cache.basic.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.cache.AcquisitionPolicy;
import org.codehaus.wadi.cache.Cache;
import org.codehaus.wadi.cache.CacheException;
import org.codehaus.wadi.cache.CacheTransaction;
import org.codehaus.wadi.cache.PutPolicy;
import org.codehaus.wadi.cache.UpdateAcquisitionPolicy;
import org.codehaus.wadi.cache.basic.entry.CacheEntry;
import org.codehaus.wadi.cache.basic.entry.GlobalObjectStore;
import org.codehaus.wadi.cache.basic.tx.BasicCacheTransaction;
import org.codehaus.wadi.cache.basic.tx.InTxCache;
import org.codehaus.wadi.cache.basic.tx.InTxCacheFactory;
import org.codehaus.wadi.cache.basic.tx.InternalCacheTransaction;
import org.codehaus.wadi.cache.policy.OptimisticAcquisitionPolicy;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class BasicCache implements Cache {
    
    private final GlobalObjectStore globalObjectStore;
    private final InternalCacheTransaction cacheTransaction;
    
    public BasicCache(GlobalObjectStore globalObjectStore, InTxCacheFactory inTxCacheFactory) {
        if (null == globalObjectStore) {
            throw new IllegalArgumentException("globalObjectStore is required");
        } else if (null == inTxCacheFactory) {
            throw new IllegalArgumentException("inTxCacheFactory is required");
        }
        this.globalObjectStore = globalObjectStore;
        
        cacheTransaction = newCacheTransaction(inTxCacheFactory);
    }

    public Object get(Object key, AcquisitionPolicy policy) throws CacheException {
    	InTxCache inTxCache = cacheTransaction.getInTxCache();
    	CacheEntry entry = inTxCache.getEntry(key);

        if (null != entry) {
            entry = entry.acquire(policy);
    		inTxCache.updateEntry(key, entry);
    	} else {
    	    entry = globalObjectStore.acquire(key, policy);
    	    inTxCache.addEntry(key, entry);
    	}
    	
        return entry.getObjectInfo().getObject();
    }

    public Map<Object, Object> get(Collection<Object> keys, AcquisitionPolicy policy) throws CacheException {
        Map<Object, Object> result = new HashMap<Object, Object>();
        for (Object key : keys) {
            Object object = get(key, policy);
            result.put(key, object);
        }
        return result;
    }
    
    public void insert(Object key, Object value, PutPolicy policy) throws CacheException {
        InTxCache inTxCache = cacheTransaction.getInTxCache();
        CacheEntry entry = inTxCache.getEntry(key);

        if (null != entry) {
            entry.insert(value);
            inTxCache.updateEntry(key, entry);
        } else {
            entry = globalObjectStore.acquire(key, OptimisticAcquisitionPolicy.DEFAULT);
            entry.insert(value);
            inTxCache.addEntry(key, entry);
        }
    }

    public void insert(Map<Object, Object> keyToValue, PutPolicy policy) throws CacheException {
        for (Map.Entry<Object, Object> entry : keyToValue.entrySet()) {
            insert(entry.getKey(), entry.getValue(), policy);
        }
    }
    
    public void update(Object key) throws CacheException {
        update(Collections.singleton(key));
    }
    
    public void update(Collection<Object> keys) throws CacheException {
        InTxCache inTxCache = cacheTransaction.getInTxCache();
        for (Object key : keys) {
            CacheEntry cacheEntry = inTxCache.getRequiredEntry(key);
            cacheEntry.update();
        }
    }
    
    public void update(Object key, Object value) throws CacheException {
        update(Collections.singletonMap(key, value));
    }

    public void update(Map<Object, Object> keyToValues) throws CacheException {
        InTxCache inTxCache = cacheTransaction.getInTxCache();
        for (Map.Entry<Object, Object> entry : keyToValues.entrySet()) {
            CacheEntry cacheEntry = inTxCache.getRequiredEntry(entry.getKey());
            cacheEntry.update(entry.getValue());
        }
    }

    public Object delete(Object key, UpdateAcquisitionPolicy policy) throws CacheException {
        InTxCache inTxCache = cacheTransaction.getInTxCache();
        CacheEntry entry = inTxCache.getEntry(key);

        if (null != entry) {
            entry = entry.acquire(policy.toAcquisitionPolicy());
            inTxCache.updateEntry(key, entry);
        } else {
            entry = globalObjectStore.acquire(key, policy.toAcquisitionPolicy());
            inTxCache.addEntry(key, entry);
        }
        
        entry.delete();
        
        return entry.getObjectInfo().getObject();
    }
    
    public Map<Object, Object> delete(Collection<Object> keys, UpdateAcquisitionPolicy policy) throws CacheException {
        Map<Object, Object> deletedEntries = new HashMap<Object, Object>();
        for (Object key : keys) {
            Object deletedEntry = delete(key, policy);
            deletedEntries.put(key, deletedEntry);
        }
        return deletedEntries;
    }
    
    public CacheTransaction getCacheTransaction() {
        return cacheTransaction;
    }

    protected InternalCacheTransaction newCacheTransaction(InTxCacheFactory inTxCacheFactory) {
        return new BasicCacheTransaction(inTxCacheFactory);
    }
    
}
