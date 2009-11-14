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

import org.codehaus.wadi.cache.basic.ObjectInfo;
import org.codehaus.wadi.cache.basic.ObjectInfoEntry;
import org.codehaus.wadi.cache.basic.OptimisticUpdateException;
import org.codehaus.wadi.cache.basic.entry.CacheEntry;
import org.codehaus.wadi.cache.basic.entry.CacheEntryState;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class CheckConflictPhaseTest extends RMockTestCase {

    private CacheEntry entry;
    private CheckConflictPhase phase;

    @Override
    protected void setUp() throws Exception {
        entry = (CacheEntry) mock(CacheEntry.class);
        phase = new CheckConflictPhase();
    }
    
    public void testExecuteDoesNotCheckConflictForCleanEntry() throws Exception {
        executeDoesNotCheckConflict(CacheEntryState.CLEAN);
    }

    public void testExecuteDoesNotCheckConflictForReadOnlyEntry() throws Exception {
        executeDoesNotCheckConflict(CacheEntryState.READ_ONLY);
    }
    
    public void testExecuteDoesNotCheckConflictForInsertedEntry() throws Exception {
        executeDoesNotCheckConflict(CacheEntryState.INSERTED);
    }
    
    public void testExecuteChecksConflictForUpdatedEntry() throws Exception {
        executeChecksConflict(CacheEntryState.UPDATED);
    }

    public void testExecuteChecksConflictForDeletedEntry() throws Exception {
        executeChecksConflict(CacheEntryState.DELETED);
    }
    
    public void testConflictThrowsOUE() throws Exception {
        recordState(CacheEntryState.UPDATED);

        recordExclusiveObjectInfoEntryAndObjectInfo(2, 2);

        startVerification();

        try {
            phase.execute(Collections.singletonMap((Object) "key", entry));
            fail();
        } catch (OptimisticUpdateException e) {
        }
    }
    
    private void executeDoesNotCheckConflict(CacheEntryState state) {
        recordState(state);

        startVerification();
        
        phase.execute(Collections.singletonMap((Object) "key", entry));
    }

    private void executeChecksConflict(CacheEntryState state) {
        recordState(state);

        recordExclusiveObjectInfoEntryAndObjectInfo(1, 2);

        startVerification();
        
        phase.execute(Collections.singletonMap((Object) "key", entry));
    }

    private void recordExclusiveObjectInfoEntryAndObjectInfo(int exclusiveVersion, int newVersion) {
        entry.getExclusiveObjectInfoEntry();
        ObjectInfo objectInfo = new ObjectInfo(exclusiveVersion, new Object());
        ObjectInfoEntry objectInfoEntry = new ObjectInfoEntry("key", objectInfo);
        modify().multiplicity(expect.from(1)).returnValue(objectInfoEntry);

        entry.getObjectInfo();
        ObjectInfo expectedObjectInfo = new ObjectInfo(newVersion, new Object());
        modify().multiplicity(expect.from(1)).returnValue(expectedObjectInfo);
    }
    
    private void recordState(CacheEntryState state) {
        entry.getState();
        modify().returnValue(state);
    }

}
