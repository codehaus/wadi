/**
 * Copyright 2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.codehaus.wadi.core;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.codehaus.wadi.core.motable.Motable;

/**
 * 
 * @version $Revision: 1538 $
 */
public class JDK5ConcurrentMotableMap implements ConcurrentMotableMap {
    private final ConcurrentHashMap<String, Motable> delegate = new ConcurrentHashMap<String, Motable>();

    public Motable acquire(String id) {
        Motable motable = delegate.get(id);
        if (null != motable) {
            try {
                getSharedLock(motable).lockInterruptibly();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            motable = delegate.get(id);
        }
        return motable;
    }
    
    public Motable acquireExclusive(String id, long exclusiveSessionLockWaitTime) {
        Motable motable = delegate.get(id);
        if (null != motable) {
            boolean success;
            try {
                success = getExclusiveLock(motable).tryLock(exclusiveSessionLockWaitTime, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            if (!success) {
                throw new MotableBusyException("Cannot obtain exclusive lock for Motable [" + id + "] after [" + 
                    exclusiveSessionLockWaitTime + "]ms.");
            }
            motable = delegate.get(id);
        }
        return motable;
    }

    public Set getNames() {
        return delegate.keySet();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public void put(String name, Motable motable) {
        delegate.put(name, motable);
    }

    public void release(Motable motable) {
        getSharedLock(motable).unlock();
    }

    public void releaseExclusive(Motable motable) {
        getExclusiveLock(motable).unlock();
    }
    
    public void remove(String name) {
        delegate.remove(name);
    }

    public int size() {
        return delegate.size();
    }
    
    protected Lock getSharedLock(Motable motable) {
        return motable.getReadWriteLock().readLock();
    }

    protected Lock getExclusiveLock(Motable motable) {
        return motable.getReadWriteLock().writeLock();
    }

}
