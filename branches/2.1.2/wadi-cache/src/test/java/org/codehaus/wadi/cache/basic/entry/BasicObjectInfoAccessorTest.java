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
import org.codehaus.wadi.cache.basic.CacheInvocation;
import org.codehaus.wadi.cache.basic.ObjectInfo;
import org.codehaus.wadi.cache.basic.ObjectInfoEntry;
import org.codehaus.wadi.cache.basic.SessionUtil;
import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.session.Session;

import com.agical.rmock.core.Action;
import com.agical.rmock.core.MethodHandle;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class BasicObjectInfoAccessorTest extends RMockTestCase {

    private Manager manager;
    private AccessListener accessListener;
    private BasicObjectInfoAccessor accessor;
    private Object key;

    @Override
    protected void setUp() throws Exception {
        key = "key";
        manager = (Manager) mock(Manager.class);
        accessListener = (AccessListener) mock(AccessListener.class);

        accessor = new BasicObjectInfoAccessor(accessListener, manager);
    }
    
    public void testReleaseExclusive() throws Exception {
        ObjectInfoEntry objectInfoEntry = recordContextualiseInvocation(ReleaseExclusiveLockInvocation.class);
        accessListener.exitExclusiveAccess(objectInfoEntry);
        
        startVerification();
        
        accessor.releaseExclusiveLock(key);
    }
    
    public void testAcquireReadOnly() throws Exception {
        ObjectInfoEntry objectInfoEntry = recordContextualiseInvocation(CacheInvocation.class);
        accessListener.enterReadOnlyAccess(objectInfoEntry);
        
        startVerification();
        
        accessor.acquireReadOnly(key, AcquisitionInfo.DEFAULT);
    }
    
    public void testAcquireOptimistic() throws Exception {
        ObjectInfoEntry objectInfoEntry = recordContextualiseInvocation(CacheInvocation.class);
        accessListener.enterOptimisticAccess(objectInfoEntry);
        
        startVerification();
        
        accessor.acquireOptimistic(key, AcquisitionInfo.DEFAULT);
    }
    
    public void testAcquirePessimistic() throws Exception {
        ObjectInfoEntry objectInfoEntry = recordContextualiseInvocation(AcquireExclusiveLockInvocation.class);
        accessListener.enterExclusiveAccess(objectInfoEntry);
        
        startVerification();
        
        accessor.acquirePessimistic(key, AcquisitionInfo.DEFAULT);
    }
    
    private ObjectInfoEntry recordContextualiseInvocation(Class invocationClass) throws Exception {
        final Session session = (Session) mock(Session.class);
        session.isNew();

        ObjectInfoEntry objectInfoEntry = new ObjectInfoEntry(key, new ObjectInfo(1, new Object()));
        SessionUtil.getObjectInfoEntry(session);
        modify().multiplicity(expect.from(1)).args(is.ANYTHING).returnValue(objectInfoEntry);

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
        
        return objectInfoEntry; 
    }

}
