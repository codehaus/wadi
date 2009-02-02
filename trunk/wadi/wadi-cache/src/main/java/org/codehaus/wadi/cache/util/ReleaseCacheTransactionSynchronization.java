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

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.codehaus.wadi.cache.CacheTransaction;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class ReleaseCacheTransactionSynchronization implements Synchronization {

    private final CacheTransaction cacheTransaction;

    public ReleaseCacheTransactionSynchronization(CacheTransaction cacheTransaction) {
        if (null == cacheTransaction) {
            throw new IllegalArgumentException("cacheTransaction is required");
        }
        this.cacheTransaction = cacheTransaction;
    }

    public void afterCompletion(int status) {
        if (status == Status.STATUS_ROLLEDBACK) {
            cacheTransaction.rollback();
        } else if (status == Status.STATUS_COMMITTED) {
            cacheTransaction.commit();
        }
    }

    public void beforeCompletion() {
    }

}
