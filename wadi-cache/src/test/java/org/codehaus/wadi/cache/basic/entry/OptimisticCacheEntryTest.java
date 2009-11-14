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

package org.codehaus.wadi.cache.basic.entry;

import org.codehaus.wadi.cache.AcquisitionInfo;
import org.codehaus.wadi.cache.basic.ObjectInfo;
import org.codehaus.wadi.cache.basic.ObjectInfoEntry;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class OptimisticCacheEntryTest extends BaseCacheEntryTestCase {

    private OptimisticCacheEntry entry;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        entry = new OptimisticCacheEntry(prototype, new ObjectInfo(1, new Object()));
    }
    
    public void testAcquireNotForPessimisticReturnsMe() throws Exception {
        policy.isAcquireForPessimisticUpdate();
        modify().returnValue(false);
        
        startVerification();
        
        CacheEntry returnedEntry = entry.acquire(policy);
        assertSame(entry, returnedEntry);
    }
    
    public void testAcquirePessimisticUpgradeToPessimisticEntryAndStateIsRetained() throws Exception {
        String key = "key";

        ObjectInfoEntry objectInfoEntry = new ObjectInfoEntry(key, new ObjectInfo(1, new Object()));
        objectInfoAccessor.acquirePessimistic(key, null);
        modify().args(is.AS_RECORDED, is.NOT_NULL).returnValue(objectInfoEntry);
        
        policy.isAcquireForPessimisticUpdate();
        modify().returnValue(true);
        
        policy.getAcquisitionInfo();
        modify().returnValue(AcquisitionInfo.DEFAULT);
        
        startVerification();
        
        entry = new OptimisticCacheEntry(prototype, new ObjectInfo(1, new Object()));
        entry.insert(new Object());

        CacheEntry returnedEntry = entry.acquire(policy);
        assertTrue(returnedEntry instanceof PessimisticCacheEntry);
        assertSame(objectInfoEntry, returnedEntry.getExclusiveObjectInfoEntry());
        assertSame(CacheEntryState.INSERTED, returnedEntry.getState());
    }
    
}
