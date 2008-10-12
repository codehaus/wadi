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

import org.codehaus.wadi.cache.AcquisitionPolicy;
import org.codehaus.wadi.cache.Cache;
import org.codehaus.wadi.cache.CacheException;
import org.codehaus.wadi.cache.CacheTransaction;
import org.codehaus.wadi.cache.PutPolicy;
import org.codehaus.wadi.cache.UpdateAcquisitionPolicy;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class TxDecoratorCache implements Cache {
    
    private final Cache cache;

    public TxDecoratorCache(Cache cache) {
        if (null == cache) {
            throw new IllegalArgumentException("cache is required");
        }
        this.cache = cache;
    }

    public Object get(final String key, final AcquisitionPolicy policy) throws CacheException {
        TransactionalOperation<Object> txOperation = new TransactionalOperation<Object>(cache) {
            @Override
            public Object doExecute() {
                return cache.get(key, policy);
            }
        };
        return txOperation.execute();
    }

    public Map<String, Object> get(final Collection<String> keys, final AcquisitionPolicy policy) throws CacheException {
        TransactionalOperation<Map<String, Object>> txOperation = new TransactionalOperation<Map<String, Object>>(cache) {
            @Override
            public Map<String, Object> doExecute() {
                return cache.get(keys, policy);
            }
        };
        return txOperation.execute();
    }
    
    public void insert(final String key, final Object value, final PutPolicy policy) throws CacheException {
        TransactionalOperation<Object> txOperation = new TransactionalOperation<Object>(cache) {
            @Override
            public Object doExecute() {
                cache.insert(key, value, policy);
                return null;
            }
        };
        txOperation.execute();
    }

    public void insert(final Map<String, Object> keyToValue, final PutPolicy policy) throws CacheException {
        TransactionalOperation<Object> txOperation = new TransactionalOperation<Object>(cache) {
            @Override
            public Object doExecute() {
                cache.insert(keyToValue, policy);
                return null;
            }
        };
        txOperation.execute();
    }
    
    public void update(final String key) throws CacheException {
        TransactionalOperation<Object> txOperation = new TransactionalOperation<Object>(cache) {
            @Override
            public Object doExecute() {
                cache.update(key);
                return null;
            }
        };
        txOperation.execute();
    }
    
    public void update(final Collection<String> keys) throws CacheException {
        TransactionalOperation<Object> txOperation = new TransactionalOperation<Object>(cache) {
            @Override
            public Object doExecute() {
                cache.update(keys);
                return null;
            }
        };
        txOperation.execute();
    }
    
    public void update(final String key, final Object value) throws CacheException {
        TransactionalOperation<Object> txOperation = new TransactionalOperation<Object>(cache) {
            @Override
            public Object doExecute() {
                cache.update(key, value);
                return null;
            }
        };
        txOperation.execute();
    }

    public void update(final Map<String, Object> keyToValues) throws CacheException {
        TransactionalOperation<Object> txOperation = new TransactionalOperation<Object>(cache) {
            @Override
            public Object doExecute() {
                cache.update(keyToValues);
                return null;
            }
        };
        txOperation.execute();
    }

    public void delete(final String key, final UpdateAcquisitionPolicy policy) throws CacheException {
        TransactionalOperation<Object> txOperation = new TransactionalOperation<Object>(cache) {
            @Override
            protected Object doExecute() {
                cache.delete(key, policy);
                return null;
            }
        };
        txOperation.execute();
    }
    
    public void delete(final Collection<String> keys, final UpdateAcquisitionPolicy policy) throws CacheException {
        TransactionalOperation<Object> txOperation = new TransactionalOperation<Object>(cache) {
            @Override
            protected Object doExecute() {
                cache.delete(keys, policy);
                return null;
            }
        };
        txOperation.execute();
    }
    
    public CacheTransaction getCacheTransaction() {
        return cache.getCacheTransaction();
    }

}
