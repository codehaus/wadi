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
import org.codehaus.wadi.cache.basic.ObjectInfo;
import org.codehaus.wadi.cache.basic.ObjectInfoEntry;
import org.codehaus.wadi.cache.basic.TimeoutException;


/**
 * @version $Rev:$ $Date:$
 */
public abstract class AbstractCacheEntry implements CacheEntry {
    protected final ObjectInfoAccessor objectInfoAccessor;
    protected final GlobalObjectStore globalObjectStore;
    protected final Object key;
    protected ObjectInfo objectInfo;
    protected CacheEntryState state;
    protected ObjectInfoEntry exclusiveObjectInfoEntry;
    
    public AbstractCacheEntry(ObjectInfoAccessor objectInfoAccessor,
            GlobalObjectStore globalObjectStore,
            Object key,
            CacheEntryState state) {
        if (null == objectInfoAccessor) {
            throw new IllegalArgumentException("objectInfoAccessor is required");
        } else if (null == globalObjectStore) {
            throw new IllegalArgumentException("globalObjectStore is required");
        } else if (null == key) {
            throw new IllegalArgumentException("key is required");
        } else if (null == state) {
            throw new IllegalArgumentException("state is required");
        }
        this.objectInfoAccessor = objectInfoAccessor;
        this.globalObjectStore = globalObjectStore;
        this.key = key;
        this.state = state;
    }

    public AbstractCacheEntry(AbstractCacheEntry prototype, ObjectInfo objectInfo, CacheEntryState state) {
        if (null == prototype) {
            throw new IllegalArgumentException("prototype is required");
        } else if (null == state) {
            throw new IllegalArgumentException("state is required");
        }
        this.state = state;
        this.objectInfo = objectInfo;

        objectInfoAccessor = prototype.objectInfoAccessor;
        globalObjectStore = prototype.globalObjectStore;
        key = prototype.key;
    }

    public CacheEntryState getState() {
        return state;
    }

    public ObjectInfo getObjectInfo() {
        if (null == objectInfo) {
            throw new IllegalStateException("ObjectInfo is null");
        }
        return objectInfo;
    }

    public void acquireExclusiveLock() throws TimeoutException {
        exclusiveObjectInfoEntry = objectInfoAccessor.acquirePessimistic(key, AcquisitionInfo.EXCLUSIVE_LOCAL_INFO);
    }
    
    public void releaseExclusiveLock() {
        if (null == exclusiveObjectInfoEntry) {
            return;
        }
        objectInfoAccessor.releaseExclusiveLock(key);
    }
    
    public ObjectInfoEntry getExclusiveObjectInfoEntry() {
        if (null == exclusiveObjectInfoEntry) {
            throw new IllegalStateException("exclusiveObjectInfo is null. Exclusive lock has not been acquired.");
        }
        return exclusiveObjectInfoEntry;
    }
}