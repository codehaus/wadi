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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

import org.codehaus.wadi.cache.AcquisitionPolicy;
import org.codehaus.wadi.cache.basic.commitphase.CommitPhase;
import org.codehaus.wadi.cache.basic.core.BasicGlobalObjectStore;
import org.codehaus.wadi.cache.basic.entry.AccessListener;
import org.codehaus.wadi.cache.basic.entry.CacheEntry;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.util.Streamer;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class BasicGlobalObjectStoreTest extends RMockTestCase {

    private AccessListener accessListener;
    private Manager manager;
    private BasicGlobalObjectStore store;
    private CacheEntry entry;
    private Streamer streamer;

    @Override
    protected void setUp() throws Exception {
        accessListener = (AccessListener) mock(AccessListener.class);
        manager = (Manager) mock(Manager.class);
        streamer = (Streamer) mock(Streamer.class);
        entry = (CacheEntry) mock(CacheEntry.class);
        store = new BasicGlobalObjectStore(accessListener, manager, streamer) {
            @Override
            protected CacheEntry newCacheEntry(String key) {
                return entry;
            }
        };
    }
    
    public void testAcquire() throws Exception {
        AcquisitionPolicy policy = (AcquisitionPolicy) mock(AcquisitionPolicy.class);
        
        CacheEntry expectedEntry = entry.acquire(policy);
        
        startVerification();
        
        CacheEntry acquiredEntry = store.acquire("key", policy);
        assertSame(expectedEntry, acquiredEntry);
    }

    public void testCommit() throws Exception {
        LinkedHashMap<String, CacheEntry> keyToEntry = new LinkedHashMap<String, CacheEntry>();

        CacheEntry entry2 = (CacheEntry) mock(CacheEntry.class);
        CacheEntry entry1 = (CacheEntry) mock(CacheEntry.class);
        keyToEntry.put("2", entry2);
        keyToEntry.put("1", entry1);
        
        final CommitPhase commitPhase = (CommitPhase) mock(CommitPhase.class);
        
        commitPhase.execute(new TreeMap<String, CacheEntry>(keyToEntry));
        
        entry2.releaseExclusiveLock();
        entry1.releaseExclusiveLock();
        
        startVerification();
        
        store = new BasicGlobalObjectStore(accessListener, manager, streamer) {
            @Override
            protected List<CommitPhase> newCommitPhases() {
                return Collections.singletonList(commitPhase);
            }
        };
        store.commit(keyToEntry);
    }

    public void testRollback() throws Exception {
        entry.releaseExclusiveLock();
        
        startVerification();
        
        store.rollback(Collections.singletonMap("key", entry));
    }
    
}
