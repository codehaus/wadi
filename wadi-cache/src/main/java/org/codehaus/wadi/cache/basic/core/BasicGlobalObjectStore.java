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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.codehaus.wadi.cache.AcquisitionPolicy;
import org.codehaus.wadi.cache.basic.OptimisticUpdateException;
import org.codehaus.wadi.cache.basic.TimeoutException;
import org.codehaus.wadi.cache.basic.commitphase.CheckConflictPhase;
import org.codehaus.wadi.cache.basic.commitphase.CommitPhase;
import org.codehaus.wadi.cache.basic.commitphase.DestroyPhase;
import org.codehaus.wadi.cache.basic.commitphase.InsertPhase;
import org.codehaus.wadi.cache.basic.commitphase.LockExclusivePhase;
import org.codehaus.wadi.cache.basic.commitphase.MergePhase;
import org.codehaus.wadi.cache.basic.entry.AccessListener;
import org.codehaus.wadi.cache.basic.entry.CacheEntry;
import org.codehaus.wadi.cache.basic.entry.GlobalObjectStore;
import org.codehaus.wadi.cache.basic.entry.ReadOnlyCacheEntry;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.util.Streamer;

/**
 * @version $Rev:$ $Date:$
 */
public class BasicGlobalObjectStore implements GlobalObjectStore {
    private final AccessListener accessListener;
    private final Manager manager;
    private final Streamer streamer;
    private final List<CommitPhase> commitPhases;

    public BasicGlobalObjectStore(AccessListener accessListener, Manager manager, Streamer streamer) {
        if (null == accessListener) {
            throw new IllegalArgumentException("accessListener is required");
        } else if (null == manager) {
            throw new IllegalArgumentException("manager is required");
        } else if (null == streamer) {
            throw new IllegalArgumentException("streamer is required");
        }
        this.accessListener = accessListener;
        this.manager = manager;
        this.streamer = streamer;
        
        commitPhases = newCommitPhases();
    }

    public CacheEntry acquire(String key, AcquisitionPolicy policy) {
        CacheEntry entry = newCacheEntry(key);
        return entry.acquire(policy);
    }

    public void commit(Map<String, CacheEntry> keyToEntry) throws TimeoutException, OptimisticUpdateException {
        keyToEntry = new TreeMap<String, CacheEntry>(keyToEntry);        
        try {
            for (CommitPhase commitPhase : commitPhases) {
                commitPhase.execute(keyToEntry);
            }
        } finally {
            for (CacheEntry cacheEntry : keyToEntry.values()) {
                cacheEntry.releaseExclusiveLock();
            }
        }
    }

    public void rollback(Map<String, CacheEntry> keyToEntry) {
        for (CacheEntry cacheEntry : keyToEntry.values()) {
            cacheEntry.releaseExclusiveLock();
        }
    }

    protected CacheEntry newCacheEntry(String key) {
        return new ReadOnlyCacheEntry(manager, accessListener, this, streamer, key);
    }

    protected List<CommitPhase> newCommitPhases() {
        List<CommitPhase> commitPhases = new ArrayList<CommitPhase>();
        commitPhases.add(new LockExclusivePhase());
        commitPhases.add(new CheckConflictPhase());
        commitPhases.add(new InsertPhase(manager));
        commitPhases.add(new DestroyPhase(manager));
        commitPhases.add(new MergePhase(manager));
        return commitPhases;
    }

}
