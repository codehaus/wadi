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


/**
 * @version $Rev:$ $Date:$
 */
public class OptimisticCacheEntry extends AbstractUpdatableCacheEntry {

    public OptimisticCacheEntry(ReadOnlyCacheEntry prototype, ObjectInfo objectInfo) {
        super(prototype, objectInfo);
    }

    public CacheEntry acquire(AcquisitionPolicy policy) throws CacheEntryException {
        if (policy.isAcquireForPessimisticUpdate()) {
            AcquisitionInfo acquisitionInfo = policy.getAcquisitionInfo();
            return upgradeToUpdatePessimistic(acquisitionInfo);
        }
        return this;
    }

    protected CacheEntry upgradeToUpdatePessimistic(AcquisitionInfo acquisitionInfo) {
        ObjectInfoEntry exclusiveObjectInfoEntry = acquirePessimistic(key, acquisitionInfo);
        return new PessimisticCacheEntry(this, objectInfo, exclusiveObjectInfoEntry);
    }
}
