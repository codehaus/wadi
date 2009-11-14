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

package org.codehaus.wadi.cache.basic.tx;

import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.wadi.cache.ExistingTransactionException;
import org.codehaus.wadi.cache.NoTransactionException;
import org.codehaus.wadi.cache.TransactionException;
import org.codehaus.wadi.cache.basic.OptimisticUpdateException;
import org.codehaus.wadi.cache.basic.TimeoutException;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class BasicCacheTransaction implements InternalCacheTransaction {
    private final InTxCacheFactory inTxCacheFactory;
    public final ConcurrentHashMap<Thread, InTxCache> threadToInTxCache;

    public BasicCacheTransaction(InTxCacheFactory inTxCacheFactory) {
        if (null == inTxCacheFactory) {
            throw new IllegalArgumentException("inTxCacheFactory is required");
        }
        this.inTxCacheFactory = inTxCacheFactory;

        threadToInTxCache = new ConcurrentHashMap<Thread, InTxCache>();
    }

    public void begin() throws TransactionException {
        Thread currentThread = Thread.currentThread();
        if (threadToInTxCache.containsKey(currentThread)) {
            throw new ExistingTransactionException();
        } else {
            InTxCache inTxCache = inTxCacheFactory.newInTxCache();
            threadToInTxCache.put(currentThread, inTxCache);
        }
    }

    public void commit() throws NoTransactionException, TimeoutException, OptimisticUpdateException {
        Thread currentThread = Thread.currentThread();
        InTxCache inTxCache = threadToInTxCache.remove(currentThread);
        if (null == inTxCache) {
            throw new NoTransactionException();
        }
        inTxCache.commit();
    }

    public void rollback() throws NoTransactionException {
        Thread currentThread = Thread.currentThread();
        InTxCache inTxCache = threadToInTxCache.remove(currentThread);
        if (null == inTxCache) {
            throw new NoTransactionException();
        }
        inTxCache.rollback();
    }
    
    public boolean isActive() {
        return threadToInTxCache.containsKey(Thread.currentThread());   
    }

    public InTxCache getInTxCache() throws NoTransactionException {
        Thread currentThread = Thread.currentThread();
        InTxCache inTxCache = threadToInTxCache.get(currentThread);
        if (null == inTxCache) {
            throw new NoTransactionException();
        }
        return inTxCache;
    }
    
}