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

package org.codehaus.wadi.core.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.codehaus.wadi.core.util.LoggingLock.IdAccessor;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class LoggingReadWriteLock implements ReadWriteLock {
    private final IdAccessor accessor;
    private final ReadWriteLock delegate;

    public LoggingReadWriteLock(IdAccessor accessor, ReadWriteLock delegate) {
        if (null == accessor) {
            throw new IllegalArgumentException("accessor is required");
        } else if (null == delegate) {
            throw new IllegalArgumentException("delegate is required");
        }
        this.accessor = accessor;
        this.delegate = delegate;
    }

    public Lock readLock() {
        return new LoggingLock("ReadLock[" + accessor.getId() + "]", delegate.readLock());
    }

    public Lock writeLock() {
        return new LoggingLock("WriteLock[" + accessor.getId() + "]", delegate.writeLock());
    }
    
    
}