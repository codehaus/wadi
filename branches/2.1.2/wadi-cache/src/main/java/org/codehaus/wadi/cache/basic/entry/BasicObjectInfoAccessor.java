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
import org.codehaus.wadi.core.WADIRuntimeException;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.manager.Manager;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class BasicObjectInfoAccessor implements ObjectInfoAccessor {
    protected final Manager manager;
    protected final AccessListener accessListener;

    public BasicObjectInfoAccessor(AccessListener accessListener, Manager manager) {
        if (null == accessListener) {
            throw new IllegalArgumentException("accessListener is required");
        } else if (null == manager) {
            throw new IllegalArgumentException("manager is required");
        }
        this.accessListener = accessListener;
        this.manager = manager;
    }

    public ObjectInfoEntry acquirePessimistic(Object key, AcquisitionInfo acquisitionInfo) {
        CacheInvocation invocation = new AcquireExclusiveLockInvocation(key, acquisitionInfo);
        ObjectInfoEntry objectInfoEntry = fetchObjectInfoEntry(invocation);
        
        accessListener.enterExclusiveAccess(objectInfoEntry);

        return objectInfoEntry;
    }

    public ObjectInfo acquireOptimistic(Object key, AcquisitionInfo acquisitionInfo) {
        CacheInvocation invocation = new CacheInvocation(key, acquisitionInfo);
        ObjectInfoEntry objectInfoEntry = fetchObjectInfoEntry(invocation);
        
        accessListener.enterOptimisticAccess(objectInfoEntry);

        return objectInfoEntry.getObjectInfo();
    }

    public ObjectInfo acquireReadOnly(Object key, AcquisitionInfo acquisitionInfo) {
        CacheInvocation invocation = new CacheInvocation(key, acquisitionInfo);
        ObjectInfoEntry objectInfoEntry = fetchObjectInfoEntry(invocation);
        
        accessListener.enterReadOnlyAccess(objectInfoEntry);
        
        return objectInfoEntry.getObjectInfo();
    }
    
    public void releaseExclusiveLock(Object key) {
        CacheInvocation invocation = new ReleaseExclusiveLockInvocation(key, AcquisitionInfo.EXCLUSIVE_LOCAL_INFO);
        ObjectInfoEntry exclusiveObjectInfoEntry = fetchObjectInfoEntry(invocation);

        accessListener.exitExclusiveAccess(exclusiveObjectInfoEntry);
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
