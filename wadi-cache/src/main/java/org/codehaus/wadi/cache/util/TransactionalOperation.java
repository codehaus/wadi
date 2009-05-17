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

package org.codehaus.wadi.cache.util;

import org.codehaus.wadi.cache.Cache;
import org.codehaus.wadi.cache.CacheTransaction;

/**
 *
 * @version $Rev:$ $Date:$
 */
public abstract class TransactionalOperation<T> {
    private final Cache cache;

    public TransactionalOperation(Cache cache) {
        if (null == cache) {
            throw new IllegalArgumentException("cache is required");
        }
        this.cache = cache;
    }

    public T execute() {
        CacheTransaction cacheTransaction = cache.getCacheTransaction();
        
        boolean decorated = false;
        if (!cacheTransaction.isActive()) {
            decorated = true;
            cacheTransaction.begin();
        }
        
        T object;
        try {
            object = doExecute();
        } catch (RuntimeException e) {
            if (decorated) {
                cacheTransaction.rollback();
            }
            throw e;
        }
        if (decorated) {
            cacheTransaction.commit();
        }

        return object;
    }

    protected abstract T doExecute();
}