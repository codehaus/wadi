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

import org.codehaus.wadi.cache.ExistingTransactionException;
import org.codehaus.wadi.cache.NoTransactionException;
import org.codehaus.wadi.cache.basic.tx.BasicCacheTransaction;
import org.codehaus.wadi.cache.basic.tx.InTxCache;
import org.codehaus.wadi.cache.basic.tx.InTxCacheFactory;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class BasicCacheTransactionTest extends RMockTestCase {

    private InTxCacheFactory inTxCacheFactory;
    private BasicCacheTransaction cacheTransaction;

    @Override
    protected void setUp() throws Exception {
        inTxCacheFactory = (InTxCacheFactory) mock(InTxCacheFactory.class);
        cacheTransaction = new BasicCacheTransaction(inTxCacheFactory);
    }
    
    public void testBegin() throws Exception {
        inTxCacheFactory.newInTxCache();
        
        startVerification();
        
        cacheTransaction.begin();
        assertTrue(cacheTransaction.isActive());
    }
    
    public void testBeginThrowsETEWhenRunningTransaction() throws Exception {
        inTxCacheFactory.newInTxCache();
        
        startVerification();
        
        cacheTransaction.begin();
        try {
            cacheTransaction.begin();
            fail();
        } catch (ExistingTransactionException e) {
        }
    }
    
    public void testCommit() throws Exception {
        InTxCache inTxCache = inTxCacheFactory.newInTxCache();
        inTxCache.commit();
        
        startVerification();
        
        cacheTransaction.begin();
        cacheTransaction.commit();
        assertFalse(cacheTransaction.isActive());
    }
    
    public void testCommitThrowsNTEWhenNoRunningTransaction() throws Exception {
        startVerification();
        
        try {
            cacheTransaction.commit();
            fail();
        } catch (NoTransactionException e) {
        }
    }
    
    public void testRollback() throws Exception {
        InTxCache inTxCache = inTxCacheFactory.newInTxCache();
        inTxCache.rollback();
        
        startVerification();
        
        cacheTransaction.begin();
        cacheTransaction.rollback();
        assertFalse(cacheTransaction.isActive());
    }
    
    public void testRollbackThrowsNTEWhenNoRunningTransaction() throws Exception {
        startVerification();
        
        try {
            cacheTransaction.rollback();
            fail();
        } catch (NoTransactionException e) {
        }
    }
    
    public void testGetInTxCache() throws Exception {
        InTxCache inTxCache = inTxCacheFactory.newInTxCache();
        
        startVerification();
        
        cacheTransaction.begin();
        assertSame(inTxCache, cacheTransaction.getInTxCache());
    }
    
    public void testGetInTxCacheThrowsNTEWhenNoRunningTransaction() throws Exception {
        startVerification();
        
        try {
            cacheTransaction.getInTxCache();
            fail();
        } catch (NoTransactionException e) {
        }
    }
    
}
