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

import java.io.IOException;
import java.util.Collections;

import org.codehaus.wadi.cache.basic.ObjectInfo;
import org.codehaus.wadi.cache.basic.ObjectInfoEntry;
import org.codehaus.wadi.cache.basic.SessionUtil;
import org.codehaus.wadi.cache.basic.entry.CacheEntry;
import org.codehaus.wadi.cache.basic.entry.CacheEntryState;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.session.Session;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class InsertPhaseTest extends RMockTestCase {

    private CacheEntry entry;
    private InsertPhase phase;
    private Manager manager;

    @Override
    protected void setUp() throws Exception {
        entry = (CacheEntry) mock(CacheEntry.class);
        manager = (Manager) mock(Manager.class);
        phase = new InsertPhase(manager);
    }
    
    public void testExecuteDoesNotInsertCleanEntry() throws Exception {
        executeDoesNotInsert(CacheEntryState.CLEAN);
    }

    public void testExecuteDoesNotInsertReadOnlyEntry() throws Exception {
        executeDoesNotInsert(CacheEntryState.READ_ONLY);
    }
    
    public void testExecuteInsertsInsertedEntry() throws Exception {
        recordState(CacheEntryState.INSERTED);
        
        entry.getObjectInfo();
        final ObjectInfo expectedObjectInfo = new ObjectInfo(1, new Object());
        modify().multiplicity(expect.from(1)).returnValue(expectedObjectInfo);
        
        String key = "key";
        Session session = manager.createWithName(key);
        SessionUtil.setObjectInfoEntry(session, new ObjectInfoEntry(key, expectedObjectInfo));
        modify().args(is.ANYTHING, new AbstractExpression() {
            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                ObjectInfoEntry entry = (ObjectInfoEntry) arg0;
                assertSame(expectedObjectInfo, entry.getObjectInfo());
                return true;
            }
        });
        
        entry.acquireExclusiveLock();
        
        startVerification();
        
        phase.execute(Collections.singletonMap("key", entry));
    }
    
    public void testExecuteDoesNotInsertUpdatedEntry() throws Exception {
        executeDoesNotInsert(CacheEntryState.UPDATED);
    }

    public void testExecuteDoesNotInsertDeletedEntry() throws Exception {
        executeDoesNotInsert(CacheEntryState.DELETED);
    }
    
    private void executeDoesNotInsert(CacheEntryState state) {
        recordState(state);

        startVerification();
        
        phase.execute(Collections.singletonMap("key", entry));
    }

    private void recordState(CacheEntryState state) {
        entry.getState();
        modify().returnValue(state);
    }

}
