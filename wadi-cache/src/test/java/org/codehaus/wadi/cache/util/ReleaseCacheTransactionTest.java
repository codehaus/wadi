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

import org.codehaus.wadi.cache.CacheTransaction;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class ReleaseCacheTransactionTest extends RMockTestCase {

    private CacheTransaction cacheTx;
    private ReleaseCacheTransactionSynchronization synchronization;

    @Override
    protected void setUp() throws Exception {
        cacheTx = (CacheTransaction) mock(CacheTransaction.class);
        synchronization = new ReleaseCacheTransactionSynchronization(cacheTx); 
    }
    
    public void testRollbackCacheTransactionWhenTxStatusIsRolledBack() throws Exception {
        cacheTx.rollback();
        
        startVerification();
        
        synchronization.afterCompletion(Status.STATUS_ROLLEDBACK);
    }
    
    public void testCommitCacheTransactionWhenTxStatusIsCommitted() throws Exception {
        cacheTx.commit();
        
        startVerification();
        
        synchronization.afterCompletion(Status.STATUS_COMMITTED);
    }
    
}
