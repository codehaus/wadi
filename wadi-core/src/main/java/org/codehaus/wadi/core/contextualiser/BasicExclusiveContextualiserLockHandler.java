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

package org.codehaus.wadi.core.contextualiser;

import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.motable.Motable;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class BasicExclusiveContextualiserLockHandler implements ExclusiveContextualiserLockHandler {
    private final ConcurrentMotableMap map;

    public BasicExclusiveContextualiserLockHandler(ConcurrentMotableMap map) {
        if (null == map) {
            throw new IllegalArgumentException("map is required");
        }
        this.map = map;
    }

    public Motable acquire(Invocation invocation, Object id) {
        if (invocation.isAcquireLockOnInvocationStart()) {
            if (invocation.isWithExclusiveLock()) {
                return map.acquireExclusive(id, invocation.getExclusiveSessionLockWaitTime());
            } else {
                return map.acquire(id);
            }
        }

        return map.get(id);
    }

    public void release(Invocation invocation, Motable motable) {
        if (invocation.isReleaseLockOnInvocationEnd()) {
            if (invocation.isWithExclusiveLock()) {
                map.releaseExclusive(motable);
            } else {
                map.release(motable);
            }
        }
    }
    
}
