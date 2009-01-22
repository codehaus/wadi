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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class LoggingLock implements Lock {
    private static final Log LOG = LogFactory.getLog(LoggingLock.class);
    
    private final String name;
    private final Lock delegate;

    public LoggingLock(String name, Lock delegate) {
        if (null == name) {
            throw new IllegalArgumentException("name is required");
        } else if (null == delegate) {
            throw new IllegalArgumentException("delegate is required");
        }
        this.name = name;
        this.delegate = delegate;
    }

    public void lock() {
        LOG.debug(name + "-lock");
        delegate.lock();
    }

    public void lockInterruptibly() throws InterruptedException {
        LOG.debug(name + "-lockInterruptibly");
        delegate.lockInterruptibly();
    }

    public Condition newCondition() {
        return delegate.newCondition();
    }

    public boolean tryLock() {
        LOG.debug(name + "-tryLock");
        return delegate.tryLock();
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        LOG.debug(name + "-tryLock(" + time + "," + unit + ")");
        return delegate.tryLock(time, unit);
    }

    public void unlock() {
        LOG.debug(name + "-unlock");
        delegate.unlock();
    }

    public interface NameAccessor {
        String getName();
    }

}