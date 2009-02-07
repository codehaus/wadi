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

package org.codehaus.wadi.cache.basic.commitphase;

import java.util.Collections;

import org.codehaus.wadi.cache.basic.entry.CacheEntry;
import org.codehaus.wadi.cache.basic.entry.CacheEntryState;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class UnLockExclusivePhaseTest extends RMockTestCase {
    
    private CacheEntry entry;
    private UnLockExclusivePhase phase;

    @Override
    protected void setUp() throws Exception {
        entry = (CacheEntry) mock(CacheEntry.class);
        phase = new UnLockExclusivePhase();
    }
    
    public void testExecuteDoesNotReleaseExclusiveLockForCleanEntry() throws Exception {
        executeDoesNotReleaseExclusiveLock(CacheEntryState.CLEAN);
    }

    public void testExecuteDoesNotReleaseExclusiveLockForReadOnlyEntry() throws Exception {
        executeDoesNotReleaseExclusiveLock(CacheEntryState.READ_ONLY);
    }
    
    public void testExecuteReleasesExclusiveLockForInsertedEntry() throws Exception {
        executeReleasesExclusiveLock(CacheEntryState.INSERTED);
    }
    
    public void testExecuteReleasesExclusiveLockForUpdatedEntry() throws Exception {
        executeReleasesExclusiveLock(CacheEntryState.UPDATED);
    }

    public void testExecuteReleasesExclusiveLockForDeletedEntry() throws Exception {
        executeReleasesExclusiveLock(CacheEntryState.DELETED);
    }
    
    private void executeDoesNotReleaseExclusiveLock(CacheEntryState state) {
        recordState(state);

        startVerification();
        
        phase.execute(Collections.singletonMap((Object) "key", entry));
    }

    private void executeReleasesExclusiveLock(CacheEntryState state) {
        recordState(state);
        
        entry.releaseExclusiveLock();
        
        startVerification();
        
        phase.execute(Collections.singletonMap((Object) "key", entry));
    }
    
    private void recordState(CacheEntryState state) {
        entry.getState();
        modify().returnValue(state);
    }
    

}
