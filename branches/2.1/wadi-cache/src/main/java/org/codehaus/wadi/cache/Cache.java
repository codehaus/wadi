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

package org.codehaus.wadi.cache;

import java.util.Collection;
import java.util.Map;

/**
 *
 * @version $Rev:$ $Date:$
 */
public interface Cache {
	Object get(String key, AcquisitionPolicy policy) throws CacheException;
	
	Map<String, Object> get(Collection<String> keys, AcquisitionPolicy policy) throws CacheException;
	
    void insert(String key, Object value, PutPolicy policy) throws CacheException;
    
    void insert(Map<String, Object> keyToValue, PutPolicy policy) throws CacheException;

    void update(String key) throws CacheException;
    
    void update(Collection<String> keys) throws CacheException;
    
    void update(String key, Object value) throws CacheException;
    
    void update(Map<String, Object> keyToValues) throws CacheException;

    void delete(String key, UpdateAcquisitionPolicy policy) throws CacheException;
    
    void delete(Collection<String> keys, UpdateAcquisitionPolicy policy) throws CacheException;

    CacheTransaction getCacheTransaction();
}
