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

import org.codehaus.wadi.cache.basic.ObjectInfo;



/**
 * @version $Rev:$ $Date:$
 */
public abstract class AbstractUpdatableCacheEntry extends AbstractCacheEntry {

    public AbstractUpdatableCacheEntry(AbstractCacheEntry prototype, ObjectInfo objectInfo) {
        super(prototype, objectInfo, CacheEntryState.CLEAN);
    }
    
    public void insert(Object value) {
        if (!state.isInsertable()) {
            throw new IllegalStateException();
        }
        objectInfo.setObject(value);
        state = CacheEntryState.INSERTED;
    }

    public void update() {
        if (!state.isUpdatable()) {
            throw new IllegalStateException();
        }
        if (state == CacheEntryState.CLEAN) {
            state = CacheEntryState.UPDATED;
        }
    }

    public void update(Object newValue) {
        update();
        objectInfo.setObject(newValue);
    }

    public void delete() {
        state = CacheEntryState.DELETED;
    }

}