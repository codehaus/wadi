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
import org.codehaus.wadi.cache.basic.ObjectInfo;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class AbstractUpdatableCacheEntryTest extends BaseCacheEntryTestCase {

    private AbstractUpdatableCacheEntry entry;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        ObjectInfo objectInfo = new ObjectInfo(1, new Object());
        entry = new AbstractUpdatableCacheEntry(prototype, objectInfo, CacheEntryState.CLEAN) {
            public CacheEntry acquire(AcquisitionPolicy policy) throws CacheEntryException {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    public void testInsert() throws Exception {
        Object insertedObject = new Object();
        entry.insert(insertedObject);
        assertSame(CacheEntryState.INSERTED, entry.getState());
        ObjectInfo objectInfo = entry.getObjectInfo();
        assertSame(insertedObject, objectInfo.getObject());
    }
    
    public void testInsertInsertThrowsISE() throws Exception {
        Object insertedObject = new Object();
        entry.insert(insertedObject);
        try {
            entry.insert(insertedObject);
        } catch (IllegalStateException e) {
        }
    }
    
    public void testInsertUpdate() throws Exception {
        entry.insert(new Object());
        Object updatedObject = new Object();
        entry.update(updatedObject);
        assertSame(CacheEntryState.INSERTED, entry.getState());
        ObjectInfo objectInfo = entry.getObjectInfo();
        assertEquals(1, objectInfo.getVersion());
        assertSame(updatedObject, objectInfo.getObject());
    }
    
    public void testInsertDelete() throws Exception {
        entry.insert(new Object());
        entry.delete();
        assertSame(CacheEntryState.DELETED, entry.getState());
    }
    
    public void testInsertDeleteInsert() throws Exception {
        entry.insert(new Object());
        entry.delete();
        entry.insert(new Object());
        assertSame(CacheEntryState.INSERTED, entry.getState());
    }
    
    public void testUpdate() throws Exception {
        Object newValue = new Object();
        entry.update(newValue);
        assertSame(CacheEntryState.UPDATED, entry.getState());
        assertSame(newValue, entry.getObjectInfo().getObject());
    }
    
    public void testUpdateDelete() throws Exception {
        Object newValue = new Object();
        entry.update(newValue);
        entry.delete();
        assertSame(CacheEntryState.DELETED, entry.getState());
    }
    
    public void testUpdateInsertThrowsISE() throws Exception {
        entry.update(new Object());
        try {
            entry.insert(new Object());
            fail();
        } catch (IllegalStateException e) {
        }
    }
    
}
