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

import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.cache.basic.OptimisticUpdateException;
import org.codehaus.wadi.cache.basic.TimeoutException;
import org.codehaus.wadi.cache.basic.entry.CacheEntry;
import org.codehaus.wadi.cache.basic.entry.CacheEntryException;
import org.codehaus.wadi.cache.basic.entry.GlobalObjectStore;
import org.codehaus.wadi.cache.basic.tx.InTxCache;


/**
 *
 * @version $Rev:$ $Date:$
 */
public class BasicInTxCache implements InTxCache {
    private final GlobalObjectStore globalObjectStore;
    private final Map<String, CacheEntry> keyToEntry;

    public BasicInTxCache(GlobalObjectStore globalObjectStore) {
        if (null == globalObjectStore) {
            throw new IllegalArgumentException("globalObjectStore is required");
        }
        this.globalObjectStore = globalObjectStore;

        keyToEntry = new HashMap<String, CacheEntry>();
    }

    public void commit() throws TimeoutException, OptimisticUpdateException {
        globalObjectStore.commit(keyToEntry);
    }

    public void rollback() {
        globalObjectStore.rollback(keyToEntry);
    }

    public CacheEntry getRequiredEntry(String key) throws CacheEntryException {
        CacheEntry entry = keyToEntry.get(key);
        if (null == entry) {
            throw new CacheEntryException("Key [" + key + "] is not bound to InTxCache");
        }
        return entry;
    }

    public CacheEntry getEntry(String key) throws CacheEntryException {
    	return keyToEntry.get(key);
    }
    
    public void addEntry(String key, CacheEntry entry) throws CacheEntryException {
        if (keyToEntry.containsKey(key)) {
            throw new CacheEntryException("Key [" + key + "] is already bound to InTxCache");
        }
        keyToEntry.put(key, entry);
    }
    
    public void updateEntry(String key, CacheEntry entry) {
        if (!keyToEntry.containsKey(key)) {
            throw new CacheEntryException("Key [" + key + "] is not bound to InTxCache");
        }
    	keyToEntry.put(key, entry);
    }

}