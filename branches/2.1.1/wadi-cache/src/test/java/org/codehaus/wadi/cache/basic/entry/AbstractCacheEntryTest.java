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

import org.codehaus.wadi.cache.AcquisitionPolicy;
import org.codehaus.wadi.cache.basic.CacheInvocation;
import org.codehaus.wadi.cache.basic.ObjectInfo;
import org.codehaus.wadi.cache.basic.ObjectInfoEntry;
import org.codehaus.wadi.cache.basic.SessionUtil;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.session.Session;

import com.agical.rmock.core.Action;
import com.agical.rmock.core.MethodHandle;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class AbstractCacheEntryTest extends BaseCacheEntryTestCase {

    private AbstractCacheEntry entry;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        entry = new AbstractCacheEntry(prototype, new ObjectInfo(), CacheEntryState.CLEAN) {
            public CacheEntry acquire(AcquisitionPolicy policy) throws CacheEntryException {
                throw new UnsupportedOperationException();
            }

            public void delete() throws NotForUpdateException {
                throw new UnsupportedOperationException();
            }

            public void insert(Object value) throws NotForUpdateException {
                throw new UnsupportedOperationException();
            }

            public void update() throws NotForUpdateException {
                throw new UnsupportedOperationException();
            }

            public void update(Object newValue) throws NotForUpdateException {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    public void testAcquiredExclusiveLock() throws Exception {
        Session session = (Session) mock(Session.class);
        ObjectInfoEntry objectInfoEntry = recordGetObjectInfoEntry(session);

        recordContextualiseInvocation(session, AcquireExclusiveLockInvocation.class);
        accessListener.enterExclusiveAccess(objectInfoEntry);
        
        startVerification();
        
        entry.acquireExclusiveLock();
        ObjectInfoEntry exclusiveObjectInfoEntry = entry.getExclusiveObjectInfoEntry();
        assertSame(objectInfoEntry, exclusiveObjectInfoEntry);
    }

    public void testReleaseExclusiveLock() throws Exception {
        Session session = (Session) mock(Session.class);
        ObjectInfoEntry objectInfoEntry = recordGetObjectInfoEntry(session);

        recordContextualiseInvocation(session, AcquireExclusiveLockInvocation.class);
        accessListener.enterExclusiveAccess(objectInfoEntry);

        recordContextualiseInvocation(session, ReleaseExclusiveLockInvocation.class);
        accessListener.exitExclusiveAccess(objectInfoEntry);
        
        startVerification();
        
        entry.acquireExclusiveLock();
        entry.releaseExclusiveLock();
    }

    private ObjectInfoEntry recordGetObjectInfoEntry(final Session session) {
        ObjectInfoEntry objectInfoEntry = new ObjectInfoEntry("key", new ObjectInfo(1, new Object()));
        SessionUtil.getObjectInfoEntry(session);
        modify().multiplicity(expect.from(1)).args(is.ANYTHING).returnValue(objectInfoEntry);
        return objectInfoEntry;
    }

    private void recordContextualiseInvocation(final Session session, Class invocationClass) throws InvocationException {
        manager.contextualise(null);
        modify().args(is.instanceOf(invocationClass)).perform(new Action() {
            public Object invocation(Object[] arg0, MethodHandle arg1) throws Throwable {
                CacheInvocation invocation = (CacheInvocation) arg0[0];
                assertTrue(invocation.isDoNotExecuteOnEndProcessing());
                invocation.setSession(session);
                invocation.invoke(null);
                return true;
            }
        });
    }

}
