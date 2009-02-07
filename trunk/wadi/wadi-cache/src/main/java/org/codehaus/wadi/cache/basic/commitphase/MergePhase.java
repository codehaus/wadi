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

package org.codehaus.wadi.cache.basic.commitphase;

import java.util.Map;

import org.codehaus.wadi.cache.AcquisitionInfo;
import org.codehaus.wadi.cache.TransactionException;
import org.codehaus.wadi.cache.basic.CacheInvocation;
import org.codehaus.wadi.cache.basic.ObjectInfo;
import org.codehaus.wadi.cache.basic.ObjectInfoEntry;
import org.codehaus.wadi.cache.basic.entry.CacheEntry;
import org.codehaus.wadi.cache.basic.entry.CacheEntryState;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.manager.Manager;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class MergePhase implements CommitPhase {

    private final Manager manager;

    public MergePhase(Manager manager) {
        if (null == manager) {
            throw new IllegalArgumentException("manager is required");
        }
        this.manager = manager;
    }

    public void execute(Map<Object, CacheEntry> keyToEntry) throws TransactionException {
        for (Map.Entry<Object, CacheEntry> entry : keyToEntry.entrySet()) {
            Object key = entry.getKey();
            CacheEntry cacheEntry = entry.getValue();
            if (cacheEntry.getState() == CacheEntryState.UPDATED) {
                ObjectInfoEntry objectInfoEntry = cacheEntry.getExclusiveObjectInfoEntry();
                ObjectInfo newObjectInfo = cacheEntry.getObjectInfo();
                objectInfoEntry.getObjectInfo().merge(newObjectInfo);

                replicateState(key);
            }
        }
    }

    protected void replicateState(Object key) {
        try {
            manager.contextualise(new CacheInvocation(key, AcquisitionInfo.EXCLUSIVE_LOCAL_INFO));
        } catch (InvocationException e) {
        }
    }

}
