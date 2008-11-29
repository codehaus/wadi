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
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.manager.Manager;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class DestroyPhaseTest extends RMockTestCase {

    private CacheEntry entry;
    private DestroyPhase phase;
    private Manager manager;

    @Override
    protected void setUp() throws Exception {
        entry = (CacheEntry) mock(CacheEntry.class);
        manager = (Manager) mock(Manager.class);
        phase = new DestroyPhase(manager);
    }
    
    public void testExecuteDoesNotDestroyCleanEntry() throws Exception {
        executeDoesNotDestroy(CacheEntryState.CLEAN);
    }

    public void testExecuteDoesNotDestroyReadOnlyEntry() throws Exception {
        executeDoesNotDestroy(CacheEntryState.READ_ONLY);
    }
    
    public void testExecuteDoesNotDestroyInsertedEntry() throws Exception {
        executeDoesNotDestroy(CacheEntryState.READ_ONLY);
    }
    
    public void testExecuteDoesNotDestroyUpdatedEntry() throws Exception {
        executeDoesNotDestroy(CacheEntryState.UPDATED);
    }

    public void testExecuteDestroysDeletedEntry() throws Exception {
        recordState(CacheEntryState.DELETED);
        
        manager.contextualise(null);
        modify().args(is.instanceOf(DestroyCacheEntryInvocation.class));
        
        startVerification();
        
        phase.execute(Collections.singletonMap("key", entry));
    }
    
    public void testIEUponDestroyAreIgnored() throws Exception {
        recordState(CacheEntryState.DELETED);
        
        manager.contextualise(null);
        modify().args(is.instanceOf(DestroyCacheEntryInvocation.class)).throwException(new InvocationException());
        
        startVerification();
        
        phase.execute(Collections.singletonMap("key", entry));
    }
    
    private void executeDoesNotDestroy(CacheEntryState state) {
        recordState(state);

        startVerification();
        
        phase.execute(Collections.singletonMap("key", entry));
    }

    private void recordState(CacheEntryState state) {
        entry.getState();
        modify().returnValue(state);
    }

}
