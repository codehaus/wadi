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

package org.codehaus.wadi.cache.basic;

import org.codehaus.wadi.cache.AcquisitionInfo;
import org.codehaus.wadi.core.session.Session;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class CacheInvocationTest extends RMockTestCase {

    public void testDoInvokeWithoutSessionSetsUndefinedEntry() throws Exception {
        startVerification();
        
        CacheInvocation invocation = new CacheInvocation("key", AcquisitionInfo.DEFAULT);
        
        invocation.doInvoke(null);
        
        assertUndefinedEntry(invocation);
    }

    public void testDoInvokeWithSessionWithoutEntrySetsUndefinedEntry() throws Exception {
        Session session = (Session) mock(Session.class);
        
        SessionUtil.getObjectInfoEntry(session);
        modify().args(is.ANYTHING);
        
        SessionUtil.setObjectInfoEntry(session, new ObjectInfoEntry("key", new ObjectInfo()));
        modify().args(is.ANYTHING, is.NOT_NULL);

        startVerification();
        
        CacheInvocation invocation = new CacheInvocation("key", AcquisitionInfo.DEFAULT);
        invocation.setSession(session);
        
        invocation.doInvoke(null);
        
        assertUndefinedEntry(invocation);
    }

    public void testDoInvokeWithSessionWithEntrySetsEntry() throws Exception {
        Session session = (Session) mock(Session.class);
        
        SessionUtil.getObjectInfoEntry(session);
        ObjectInfoEntry expectedEntry = new ObjectInfoEntry("key", new ObjectInfo());
        modify().args(is.ANYTHING).returnValue(expectedEntry);
        
        startVerification();
        
        CacheInvocation invocation = new CacheInvocation("key", AcquisitionInfo.DEFAULT);
        invocation.setSession(session);
        
        invocation.doInvoke(null);

        ObjectInfoEntry actualEntry = invocation.getObjectInfoEntry();
        assertSame(expectedEntry, actualEntry);
    }
    
    private void assertUndefinedEntry(CacheInvocation invocation) {
        ObjectInfoEntry objectInfoEntry = invocation.getObjectInfoEntry();
        ObjectInfo objectInfo = objectInfoEntry.getObjectInfo();
        assertTrue(objectInfo.isUndefined());
        assertEquals(0, objectInfo.getVersion());
    }

}
