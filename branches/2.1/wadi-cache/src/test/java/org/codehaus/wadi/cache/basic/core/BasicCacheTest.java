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

import org.codehaus.wadi.cache.AcquisitionPolicy;
import org.codehaus.wadi.cache.UpdateAcquisitionPolicy;
import org.codehaus.wadi.cache.basic.ObjectInfo;
import org.codehaus.wadi.cache.basic.core.BasicCache;
import org.codehaus.wadi.cache.basic.entry.CacheEntry;
import org.codehaus.wadi.cache.basic.entry.GlobalObjectStore;
import org.codehaus.wadi.cache.basic.tx.InTxCache;
import org.codehaus.wadi.cache.basic.tx.InTxCacheFactory;
import org.codehaus.wadi.cache.basic.tx.InternalCacheTransaction;

import com.agical.rmock.extension.junit.RMockTestCase;


/**
 * @version $Rev:$ $Date:$
 */
public class BasicCacheTest extends RMockTestCase {

    private GlobalObjectStore globalObjectStore;
    private InTxCacheFactory inTxCacheFactory;
    private BasicCache cache;
    private InternalCacheTransaction cacheTransaction;
    private String key;

    @Override
    protected void setUp() throws Exception {
        key = "key";
        globalObjectStore = (GlobalObjectStore) mock(GlobalObjectStore.class);
        inTxCacheFactory = (InTxCacheFactory) mock(InTxCacheFactory.class);
        cacheTransaction = (InternalCacheTransaction) mock(InternalCacheTransaction.class);
        cache = new BasicCache(globalObjectStore , inTxCacheFactory) {
            @Override
            protected InternalCacheTransaction newCacheTransaction(InTxCacheFactory inTxCacheFactory) {
                return cacheTransaction;
            }
        };
    }
    
    public void testGetNotInTxCacheObject() throws Exception {
        AcquisitionPolicy policy = (AcquisitionPolicy) mock(AcquisitionPolicy.class);
        
        InTxCache inTxCache = cacheTransaction.getInTxCache();
        inTxCache.getEntry(key);
        modify().returnValue(null);

        CacheEntry entry = globalObjectStore.acquire(key, policy);
        
        inTxCache.addEntry(key, entry);
        
        Object object = recordGetObjectFromCacheEntry(entry);
        
        startVerification();
        
        Object actualObject = cache.get(key, policy);
        assertSame(object, actualObject);
    }

    public void testGetInTxCacheObject() throws Exception {
        AcquisitionPolicy policy = (AcquisitionPolicy) mock(AcquisitionPolicy.class);
        
        InTxCache inTxCache = cacheTransaction.getInTxCache();
        CacheEntry entry = inTxCache.getEntry(key);

        entry = entry.acquire(policy);
        
        inTxCache.updateEntry(key, entry);
        
        Object object = recordGetObjectFromCacheEntry(entry);
        
        startVerification();
        
        Object actualObject = cache.get(key, policy);
        assertSame(object, actualObject);
    }

    public void testUpdateWithoutValue() throws Exception {
        InTxCache inTxCache = cacheTransaction.getInTxCache();
        CacheEntry entry = inTxCache.getRequiredEntry(key);

        entry.update();
        
        startVerification();
        
        cache.update(key);
    }
    
    public void testUpdateWithValue() throws Exception {
        Object value = new Object();

        InTxCache inTxCache = cacheTransaction.getInTxCache();
        CacheEntry entry = inTxCache.getRequiredEntry(key);
        
        entry.update(value);
        
        startVerification();
        
        cache.update(key, value);
    }

    public void testDeleteNotInTxCacheObject() throws Exception {
        UpdateAcquisitionPolicy policy = (UpdateAcquisitionPolicy) mock(UpdateAcquisitionPolicy.class);

        InTxCache inTxCache = cacheTransaction.getInTxCache();
        inTxCache.getEntry(key);
        modify().returnValue(null);

        CacheEntry entry = globalObjectStore.acquire(key, policy.toAcquisitionPolicy());
        
        inTxCache.addEntry(key, entry);
        
        entry.delete();
        
        startVerification();
        
        cache.delete(key, policy);
    }
    
    public void testDeleteInTxCacheObject() throws Exception {
        UpdateAcquisitionPolicy policy = (UpdateAcquisitionPolicy) mock(UpdateAcquisitionPolicy.class);
        
        InTxCache inTxCache = cacheTransaction.getInTxCache();
        CacheEntry entry = inTxCache.getEntry(key);
        
        entry = entry.acquire(policy.toAcquisitionPolicy());
        
        inTxCache.updateEntry(key, entry);

        entry.delete();
        
        startVerification();
        
        cache.delete(key, policy);
    }
    
    private Object recordGetObjectFromCacheEntry(CacheEntry entry) {
        entry.getObjectInfo();
        Object object = new Object();
        modify().returnValue(new ObjectInfo(1, object));
        return object;
    }

}
