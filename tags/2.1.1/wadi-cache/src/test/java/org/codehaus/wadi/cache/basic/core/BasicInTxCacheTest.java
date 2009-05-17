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

import org.codehaus.wadi.cache.basic.core.BasicInTxCache;
import org.codehaus.wadi.cache.basic.entry.CacheEntry;
import org.codehaus.wadi.cache.basic.entry.CacheEntryException;
import org.codehaus.wadi.cache.basic.entry.GlobalObjectStore;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class BasicInTxCacheTest extends RMockTestCase {

    private GlobalObjectStore globalObjectStore;
    private BasicInTxCache inTxCache;

    @Override
    protected void setUp() throws Exception {
        globalObjectStore = (GlobalObjectStore) mock(GlobalObjectStore.class);
        inTxCache = new BasicInTxCache(globalObjectStore );
    }
    
    public void testCommit() throws Exception {
        globalObjectStore.commit(null);
        modify().args(is.NOT_NULL);
        
        startVerification();
        
        inTxCache.commit();
    }

    public void testRollback() throws Exception {
        globalObjectStore.rollback(null);
        modify().args(is.NOT_NULL);
        
        startVerification();
        
        inTxCache.rollback();
    }
    
    public void testGetRequiredEntry() throws Exception {
        startVerification();
        
        String key = "key";
        CacheEntry entry = (CacheEntry) mock(CacheEntry.class);
        inTxCache.addEntry(key, entry);
        assertSame(entry, inTxCache.getRequiredEntry(key));
    }
    
    public void testGetRequiredEntryThrowsCEEWhenNoEntry() throws Exception {
        startVerification();
        
        try {
            inTxCache.getRequiredEntry("key");
            fail();
        } catch (CacheEntryException e) {
        }
    }

    public void testGetEntry() throws Exception {
        startVerification();
        
        String key = "key";
        CacheEntry entry = (CacheEntry) mock(CacheEntry.class);
        inTxCache.addEntry(key, entry);
        assertSame(entry, inTxCache.getEntry(key));
    }
    
    public void testUpdateEntry() throws Exception {
        startVerification();
        
        String key = "key";
        CacheEntry entry = (CacheEntry) mock(CacheEntry.class);
        CacheEntry updatedEntry = (CacheEntry) mock(CacheEntry.class);
        inTxCache.addEntry(key, entry);
        inTxCache.updateEntry(key, updatedEntry);
        assertSame(updatedEntry, inTxCache.getEntry(key));
    }
    
    public void testUpdateEntryThrowsCEEWhenNoEntry() throws Exception {
        startVerification();
        
        CacheEntry entry = (CacheEntry) mock(CacheEntry.class);
        try {
            inTxCache.updateEntry("key", entry);
            fail();
        } catch (CacheEntryException e) {
        }
    }
    
}
