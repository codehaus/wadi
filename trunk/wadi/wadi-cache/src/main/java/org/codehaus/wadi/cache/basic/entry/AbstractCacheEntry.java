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
import org.codehaus.wadi.cache.basic.TimeoutException;
import org.codehaus.wadi.core.WADIRuntimeException;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.manager.Manager;


/**
 * @version $Rev:$ $Date:$
 */
public abstract class AbstractCacheEntry implements CacheEntry {
    protected final Manager manager;
    protected final AccessListener accessListener;
    protected final GlobalObjectStore globalObjectStore;
    protected final String key;
    protected ObjectInfo objectInfo;
    protected CacheEntryState state;
    protected ObjectInfoEntry exclusiveObjectInfoEntry;
    
    public AbstractCacheEntry(Manager manager,
            AccessListener accessListener,
            GlobalObjectStore globalObjectStore,
            String key,
            CacheEntryState state) {
        if (null == manager) {
            throw new IllegalArgumentException("manager is required");
        } else if (null == accessListener) {
            throw new IllegalArgumentException("accessListener is required");
        } else if (null == globalObjectStore) {
            throw new IllegalArgumentException("globalObjectStore is required");
        } else if (null == key) {
            throw new IllegalArgumentException("key is required");
        } else if (null == state) {
            throw new IllegalArgumentException("state is required");
        }
        this.manager = manager;
        this.accessListener = accessListener;
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

        manager = prototype.manager;
        accessListener = prototype.accessListener;
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
        exclusiveObjectInfoEntry = acquirePessimistic(key, AcquisitionInfo.EXCLUSIVE_LOCAL_INFO);
    }
    
    public void releaseExclusiveLock() {
        if (null == exclusiveObjectInfoEntry) {
            return;
        }
        CacheInvocation invocation = new ReleaseExclusiveLockInvocation(key, AcquisitionInfo.EXCLUSIVE_LOCAL_INFO);
        fetchObjectInfoEntry(invocation);
        accessListener.exitExclusiveAccess(exclusiveObjectInfoEntry);
    }
    
    public ObjectInfoEntry getExclusiveObjectInfoEntry() {
        if (null == exclusiveObjectInfoEntry) {
            throw new IllegalStateException("exclusiveObjectInfo is null. Exclusive lock has not been acquired.");
        }
        return exclusiveObjectInfoEntry;
    }

    protected ObjectInfoEntry acquirePessimistic(String key, AcquisitionInfo acquisitionInfo) {
        CacheInvocation invocation = new AcquireExclusiveLockInvocation(key, acquisitionInfo);
        ObjectInfoEntry objectInfoEntry = fetchObjectInfoEntry(invocation);
        
        accessListener.enterExclusiveAccess(objectInfoEntry);

        return objectInfoEntry;
    }

    protected ObjectInfoEntry fetchObjectInfoEntry(CacheInvocation invocation) {
        invocation.setDoNotExecuteOnEndProcessing(true);
        
        boolean contextualised;
        try {
            contextualised = manager.contextualise(invocation);
        } catch (InvocationException e) {
            throw new WADIRuntimeException(e);
        }
        if (!contextualised) {
            throw new WADIRuntimeException("Problem during cacheInvocation " + invocation);
        }
        return invocation.getObjectInfoEntry();
    }
    
}