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

import java.util.Collection;
import java.util.Map;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.codehaus.wadi.cache.AcquisitionPolicy;
import org.codehaus.wadi.cache.Cache;
import org.codehaus.wadi.cache.CacheException;
import org.codehaus.wadi.cache.CacheTransaction;
import org.codehaus.wadi.cache.PutPolicy;
import org.codehaus.wadi.cache.TransactionException;
import org.codehaus.wadi.cache.UpdateAcquisitionPolicy;


/**
 *
 * @version $Rev:$ $Date:$
 */
public class TransactionManagerAwareCache implements Cache {
    private final TransactionManager tm;
    private final Cache delegate;

    public TransactionManagerAwareCache(Cache delegate, TransactionManager tm) {
        if (null == delegate) {
            throw new IllegalArgumentException("delegate is required");
        } else if (null == tm) {
            throw new IllegalArgumentException("transactionManager is required");
        }
        this.delegate = delegate;
        this.tm = tm;
    }
    
    public Map<Object, Object> delete(Collection<Object> keys, UpdateAcquisitionPolicy policy) throws CacheException {
        synchronizeWithTransactionManagerTx();
        return delegate.delete(keys, policy);
    }

    public Object delete(Object key, UpdateAcquisitionPolicy policy) throws CacheException {
        synchronizeWithTransactionManagerTx();
        return delegate.delete(key, policy);
    }

    public Map<Object, Object> get(Collection<Object> keys, AcquisitionPolicy policy) throws CacheException {
        synchronizeWithTransactionManagerTx();
        return delegate.get(keys, policy);
    }

    public Object get(Object key, AcquisitionPolicy policy) throws CacheException {
        synchronizeWithTransactionManagerTx();
        return delegate.get(key, policy);
    }

    public CacheTransaction getCacheTransaction() {
        synchronizeWithTransactionManagerTx();
        return delegate.getCacheTransaction();
    }

    public void insert(Map<Object, Object> keyToValue, PutPolicy policy) throws CacheException {
        synchronizeWithTransactionManagerTx();
        delegate.insert(keyToValue, policy);
    }

    public void insert(Object key, Object value, PutPolicy policy) throws CacheException {
        synchronizeWithTransactionManagerTx();
        delegate.insert(key, value, policy);
    }

    public void update(Collection<Object> keys) throws CacheException {
        synchronizeWithTransactionManagerTx();
        delegate.update(keys);
    }

    public void update(Map<Object, Object> keyToValues) throws CacheException {
        synchronizeWithTransactionManagerTx();
        delegate.update(keyToValues);
    }

    public void update(Object key, Object value) throws CacheException {
        synchronizeWithTransactionManagerTx();
        delegate.update(key, value);
    }

    public void update(Object key) throws CacheException {
        synchronizeWithTransactionManagerTx();
        delegate.update(key);
    }

    protected void synchronizeWithTransactionManagerTx() {
        try {
            Transaction transaction = tm.getTransaction();
            if (null == transaction) {
                return;
            }
            int status = transaction.getStatus();
            if (status == Status.STATUS_NO_TRANSACTION || status == Status.STATUS_UNKNOWN) {
                return;
            }
            
            CacheTransaction cacheTransaction = delegate.getCacheTransaction();
            if (cacheTransaction.isActive()) {
                return;
            }
            
            cacheTransaction.begin();
            Synchronization synchronization = new ReleaseCacheTransactionSynchronization(cacheTransaction);
            transaction.registerSynchronization(synchronization);
        } catch (Exception e) {
            throw new TransactionException("See nested", e);
        }
    }

}
