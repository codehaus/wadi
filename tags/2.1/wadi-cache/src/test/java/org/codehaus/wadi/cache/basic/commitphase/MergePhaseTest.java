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
import org.codehaus.wadi.cache.basic.entry.CacheEntry;
import org.codehaus.wadi.cache.basic.entry.CacheEntryState;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class MergePhaseTest extends RMockTestCase {

    private CacheEntry entry;
    private MergePhase phase;

    @Override
    protected void setUp() throws Exception {
        entry = (CacheEntry) mock(CacheEntry.class);
        phase = new MergePhase();
    }
    
    public void testExecuteDoesNotMergeCleanEntry() throws Exception {
        executeDoesNotMerge(CacheEntryState.CLEAN);
    }

    public void testExecuteDoesNotMergeReadOnlyEntry() throws Exception {
        executeDoesNotMerge(CacheEntryState.READ_ONLY);
    }
    
    public void testExecuteDoesNotMergeInsertedEntry() throws Exception {
        executeDoesNotMerge(CacheEntryState.INSERTED);
    }
    
    public void testExecuteMergesUpdatedEntry() throws Exception {
        executeMerges(CacheEntryState.UPDATED);
    }

    public void testExecuteDoesNotMergeDeletedEntry() throws Exception {
        executeDoesNotMerge(CacheEntryState.DELETED);
    }
    
    private void executeDoesNotMerge(CacheEntryState state) {
        recordState(state);

        startVerification();
        
        phase.execute(Collections.singletonMap("key", entry));
    }

    private void executeMerges(CacheEntryState state) {
        recordState(state);

        entry.getExclusiveObjectInfoEntry();
        ObjectInfoEntry objectInfoEntry = new ObjectInfoEntry("key", new ObjectInfo(1, new Object()));
        modify().multiplicity(expect.from(1)).returnValue(objectInfoEntry);
        
        entry.getObjectInfo();
        ObjectInfo expectedObjectInfo = new ObjectInfo(2, new Object());
        modify().multiplicity(expect.from(1)).returnValue(expectedObjectInfo);

        startVerification();
        
        phase.execute(Collections.singletonMap("key", entry));
    }

    private void recordState(CacheEntryState state) {
        entry.getState();
        modify().returnValue(state);
    }

}
