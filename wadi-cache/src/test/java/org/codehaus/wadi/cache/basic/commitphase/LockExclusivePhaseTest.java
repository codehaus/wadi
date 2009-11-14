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
 * @version $Rev:$ $Date:$
 */
public class LockExclusivePhaseTest extends RMockTestCase {

    private CacheEntry entry;
    private LockExclusivePhase phase;

    @Override
    protected void setUp() throws Exception {
        entry = (CacheEntry) mock(CacheEntry.class);
        phase = new LockExclusivePhase();
    }
    
    public void testExecuteDoesNotAcquireExclusiveLockForCleanEntry() throws Exception {
        executeDoesNotAcquireExclusiveLock(CacheEntryState.CLEAN);
    }

    public void testExecuteDoesNotAcquireExclusiveLockForReadOnlyEntry() throws Exception {
        executeDoesNotAcquireExclusiveLock(CacheEntryState.READ_ONLY);
    }
    
    public void testExecuteDoesNotAcquireExclusiveLockForInsertedEntry() throws Exception {
        executeDoesNotAcquireExclusiveLock(CacheEntryState.INSERTED);
    }
    
    public void testExecuteAcquiresExclusiveLockForUpdatedEntry() throws Exception {
        executeAcquiresExclusiveLock(CacheEntryState.UPDATED);
    }

    public void testExecuteAcquiresExclusiveLockForDeletedEntry() throws Exception {
        executeAcquiresExclusiveLock(CacheEntryState.DELETED);
    }
    
    private void executeDoesNotAcquireExclusiveLock(CacheEntryState state) {
        recordState(state);

        startVerification();
        
        phase.execute(Collections.singletonMap((Object) "key", entry));
    }

    private void executeAcquiresExclusiveLock(CacheEntryState state) {
        recordState(state);
        
        entry.acquireExclusiveLock();
        
        startVerification();
        
        phase.execute(Collections.singletonMap((Object) "key", entry));
    }
    
    private void recordState(CacheEntryState state) {
        entry.getState();
        modify().returnValue(state);
    }
    
}
