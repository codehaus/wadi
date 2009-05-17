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

package org.codehaus.wadi.core.contextualiser;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.codehaus.wadi.core.motable.Motable;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class BasicMotableLockHandler implements MotableLockHandler {

    public boolean acquire(Invocation invocation, Motable motable) {
        if (invocation.isAcquireLockOnInvocationStart()) {
            Lock lock = getLock(invocation, motable);
            try {
                lock.lockInterruptibly();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    public void release(Invocation invocation, Motable motable) {
        if (invocation.isReleaseLockOnInvocationEnd()) {
            Lock lock = getLock(invocation, motable);
            lock.unlock();
        }
    }
    
    protected Lock getLock(Invocation invocation, Motable motable) {
        ReadWriteLock readWriteLock = motable.getReadWriteLock();
        if (invocation.isWithExclusiveLock()) {
            return readWriteLock.writeLock();
        }
        return readWriteLock.readLock();
    }

}
