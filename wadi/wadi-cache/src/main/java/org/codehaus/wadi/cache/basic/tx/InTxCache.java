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

import org.codehaus.wadi.cache.basic.OptimisticUpdateException;
import org.codehaus.wadi.cache.basic.TimeoutException;
import org.codehaus.wadi.cache.basic.entry.CacheEntry;
import org.codehaus.wadi.cache.basic.entry.CacheEntryException;




/**
 *
 * @version $Rev:$ $Date:$
 */
public interface InTxCache {
    CacheEntry getRequiredEntry(Object key) throws CacheEntryException;

    CacheEntry getEntry(Object key) throws CacheEntryException;

    void addEntry(Object key, CacheEntry entry) throws CacheEntryException;

    void updateEntry(Object key, CacheEntry entry) throws CacheEntryException;

    void commit() throws TimeoutException, OptimisticUpdateException;
    
    void rollback();
}
