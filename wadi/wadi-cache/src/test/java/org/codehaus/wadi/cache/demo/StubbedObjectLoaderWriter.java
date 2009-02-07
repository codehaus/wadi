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

package org.codehaus.wadi.cache.demo;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.codehaus.wadi.cache.store.ObjectLoader;
import org.codehaus.wadi.cache.store.ObjectWriter;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class StubbedObjectLoaderWriter implements ObjectLoader, ObjectWriter {
    public static final POJO OBJECT_STORE_OBJECT = new POJO();
    public static final String OBJECT_STORE_OBJECT_KEY = "key2";

    static {
        OBJECT_STORE_OBJECT.field = 123456789;
    }
    
    private final CountDownLatch waitForObjectWriteLatch;

    public StubbedObjectLoaderWriter(CountDownLatch waitForObjectWriteLatch) {
        this.waitForObjectWriteLatch = waitForObjectWriteLatch;
    }

    public Object load(Object id) {
        if (id.equals(OBJECT_STORE_OBJECT_KEY)) {
            return OBJECT_STORE_OBJECT;
        }
        return null;
    }

    public Map<Object, Object> load(Set<Object> ids) {
        throw new UnsupportedOperationException();
    }

    public void write(Object id, Object object) {
        POJO pojo = (POJO) object;
        if (pojo.field == OBJECT_STORE_OBJECT.field) {
            waitForObjectWriteLatch.countDown();
        }
    }

}
