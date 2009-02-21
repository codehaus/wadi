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
import org.codehaus.wadi.cache.AcquisitionPolicy;
import org.codehaus.wadi.cache.basic.ObjectInfo;
import org.codehaus.wadi.cache.basic.ObjectInfoEntry;
import org.codehaus.wadi.core.util.Streamer;



/**
 * @version $Rev:$ $Date:$
 */
public class ReadOnlyCacheEntry extends AbstractCacheEntry {
    private final Streamer streamer;

    public ReadOnlyCacheEntry(ObjectInfoAccessor objectInfoAccessor,
            GlobalObjectStore globalObjectStore,
            Streamer streamer,
            Object key) {
        super(objectInfoAccessor, globalObjectStore, key, CacheEntryState.READ_ONLY);
        if (null == streamer) {
            throw new IllegalArgumentException("streamer is required");
        }
        this.streamer = streamer;
    }
    
    public CacheEntry acquire(AcquisitionPolicy policy) throws CacheEntryException {
        AcquisitionInfo acquisitionInfo = policy.getAcquisitionInfo();
        if (policy.isAcquireForReadOnly()) {
            getOrFetchObject(acquisitionInfo);
            return this;
        } else if (policy.isAcquireForOptimisticUpdate()) {
            return cloneForUpdateOptimistic(acquisitionInfo);
        } else if (policy.isAcquireForPessimisticUpdate()) {
            return cloneForUpdatePessimistic(acquisitionInfo);
        }
        throw new IllegalArgumentException(policy + " is not supported");
    }
    
    public void insert(Object value) {
        throw new NotForUpdateException();
    }

    public void update() {
        throw new NotForUpdateException();
    }

    public void update(Object newValue) {
        throw new NotForUpdateException();
    }

    public void delete() {
        throw new NotForUpdateException();
    }

    protected Object getOrFetchObject(AcquisitionInfo acquisitionInfo) {
        if (null == objectInfo) {
            objectInfo = objectInfoAccessor.acquireReadOnly(key, acquisitionInfo);
        }
        return objectInfo.getObject();
    }

    protected CacheEntry cloneForUpdateOptimistic(AcquisitionInfo acquisitionInfo) {
        ObjectInfo objectInfo = objectInfoAccessor.acquireOptimistic(key, acquisitionInfo);
        objectInfo = objectInfo.incrementVersion(streamer);
        return new OptimisticCacheEntry(this, objectInfo);
    }

    protected CacheEntry cloneForUpdatePessimistic(AcquisitionInfo acquisitionInfo) {
        ObjectInfoEntry exclusiveObjectInfoEntry = objectInfoAccessor.acquirePessimistic(key, acquisitionInfo);
        ObjectInfo objectInfo = exclusiveObjectInfoEntry.getObjectInfo().incrementVersion(streamer);
        return new PessimisticCacheEntry(this, objectInfo, exclusiveObjectInfoEntry, CacheEntryState.CLEAN);
    }

}