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

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class TransactionalOperationTest extends RMockTestCase {

    private TransactionalOperation<Object> operation;
    private Cache delegate;
    private Object returnedObject;

    @Override
    protected void setUp() throws Exception {
        delegate = (Cache) mock(Cache.class);
        returnedObject = new Object();
        operation = new TransactionalOperation<Object>(delegate) {
            @Override
            protected Object doExecute() {
                return returnedObject;
            }
        };
    }

    public void testExecuteBeginAndCommitTxWhenNoTx() throws Exception {
        CacheTransaction cacheTransaction = delegate.getCacheTransaction();
        cacheTransaction.isActive();

        cacheTransaction.begin();
        
        cacheTransaction.commit();
        
        startVerification();
        
        Object actualReturnedObject = operation.execute();
        assertSame(returnedObject, actualReturnedObject);
    }
    
    public void testExecuteDoesNothingWhenTx() throws Exception {
        CacheTransaction cacheTransaction = delegate.getCacheTransaction();
        cacheTransaction.isActive();
        modify().returnValue(true);
        
        startVerification();
        
        Object actualReturnedObject = operation.execute();
        assertSame(returnedObject, actualReturnedObject);
    }
    
    public void testExecuteBeginAndRollbackWhenNoTxAndExceptionDuringDoExecute() throws Exception {
        CacheTransaction cacheTransaction = delegate.getCacheTransaction();
        cacheTransaction.isActive();

        cacheTransaction.begin();
        cacheTransaction.rollback();
        
        startVerification();
        
        operation = new TransactionalOperation<Object>(delegate) {
            @Override
            protected Object doExecute() {
                throw new IllegalStateException();
            }
        };
        try {
            operation.execute();
        } catch (IllegalStateException e) {
        }
    }
    
}
