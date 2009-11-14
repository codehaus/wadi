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
	Object get(Object key, AcquisitionPolicy policy) throws CacheException;
	
	Map<Object, Object> get(Collection<Object> keys, AcquisitionPolicy policy) throws CacheException;
	
    void insert(Object key, Object value, PutPolicy policy) throws CacheException;
    
    void insert(Map<Object, Object> keyToValue, PutPolicy policy) throws CacheException;

    void update(Object key) throws CacheException;
    
    void update(Collection<Object> keys) throws CacheException;
    
    void update(Object key, Object value) throws CacheException;
    
    void update(Map<Object, Object> keyToValues) throws CacheException;

    Object delete(Object key, UpdateAcquisitionPolicy policy) throws CacheException;
    
    Map<Object, Object>  delete(Collection<Object> keys, UpdateAcquisitionPolicy policy) throws CacheException;

    CacheTransaction getCacheTransaction();
}
