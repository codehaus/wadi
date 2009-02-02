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
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.codehaus.wadi.cache.AcquisitionPolicy;
import org.codehaus.wadi.cache.Cache;
import org.codehaus.wadi.cache.CacheTransaction;
import org.codehaus.wadi.cache.policy.ReadOnlyAcquisitionPolicy;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class TransactionManagerAwareCacheTest extends RMockTestCase {

    private Cache delegate;
    private TransactionManager tm;
    private TransactionManagerAwareCache cache;

    @Override
    protected void setUp() throws Exception {
        delegate = (Cache) mock(Cache.class);
        tm = (TransactionManager) mock(TransactionManager.class);
        cache = new TransactionManagerAwareCache(delegate, tm);
    }
    
    public void testDoesNotRegisterSynchronisationWhenNullTransaction() throws Exception {
        tm.getTransaction();
        modify().returnValue(null);

        executeDelegatedOperation();
    }
    
    public void testDoesNotRegisterSynchronisationWhenNoTransactionStatus() throws Exception {
        executeDoesNotRegisterSynchronizationForTransactionStatus(Status.STATUS_NO_TRANSACTION);
    }

    public void testDoesNotRegisterSynchronisationWhenUnknownTransactionStatus() throws Exception {
        executeDoesNotRegisterSynchronizationForTransactionStatus(Status.STATUS_UNKNOWN);
    }
    
    public void testDoesNotRegisterSynchronisationWhenActiveCacheTransaction() throws Exception {
        Transaction tx = tm.getTransaction();
        tx.getStatus();
        modify().returnValue(Status.STATUS_ACTIVE);

        CacheTransaction cacheTransaction = delegate.getCacheTransaction();
        cacheTransaction.isActive();
        modify().returnValue(true);
        
        executeDelegatedOperation();        
    }
    
    public void testRegistersSynchronisationWhenNoActiveCacheTransaction() throws Exception {
        Transaction tx = tm.getTransaction();
        tx.getStatus();
        modify().returnValue(Status.STATUS_ACTIVE);

        CacheTransaction cacheTransaction = delegate.getCacheTransaction();
        cacheTransaction.isActive();

        cacheTransaction.begin();
        tx.registerSynchronization(null);
        modify().args(is.NOT_NULL);
        
        executeDelegatedOperation();        
    }

    private void executeDoesNotRegisterSynchronizationForTransactionStatus(int status) throws SystemException {
        Transaction tx = tm.getTransaction();
        tx.getStatus();
        modify().returnValue(status);
        
        executeDelegatedOperation();
    }

    private void executeDelegatedOperation() {
        String key = "key";
        AcquisitionPolicy acquisitionPolicy = ReadOnlyAcquisitionPolicy.DEFAULT;
        delegate.get(key, acquisitionPolicy);

        startVerification();
        
        cache.get(key, acquisitionPolicy);
    }

}
