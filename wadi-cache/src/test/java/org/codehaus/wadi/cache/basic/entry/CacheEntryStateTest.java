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

import junit.framework.TestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class CacheEntryStateTest extends TestCase {

    public void testIsDirty() throws Exception {
        assertFalse(CacheEntryState.CLEAN.isDirty());
        assertTrue(CacheEntryState.DELETED.isDirty());
        assertFalse(CacheEntryState.INSERTED.isDirty());
        assertFalse(CacheEntryState.READ_ONLY.isDirty());
        assertTrue(CacheEntryState.UPDATED.isDirty());
    }
    
    public void testIsInserteable() throws Exception {
        assertTrue(CacheEntryState.CLEAN.isInsertable());
        assertTrue(CacheEntryState.DELETED.isInsertable());
        assertFalse(CacheEntryState.INSERTED.isInsertable());
        assertFalse(CacheEntryState.READ_ONLY.isInsertable());
        assertFalse(CacheEntryState.UPDATED.isInsertable());
    }
    
    public void testIsUpdatable() throws Exception {
        assertTrue(CacheEntryState.CLEAN.isUpdatable());
        assertFalse(CacheEntryState.DELETED.isUpdatable());
        assertTrue(CacheEntryState.INSERTED.isUpdatable());
        assertFalse(CacheEntryState.READ_ONLY.isUpdatable());
        assertTrue(CacheEntryState.UPDATED.isUpdatable());
    }
    
}
