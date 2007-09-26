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

import org.codehaus.wadi.core.motable.Motable;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * 
 * @version $Revision: 1538 $
 */
public class OswegoConcurrentMotableMap implements ConcurrentMotableMap {
    private final ConcurrentHashMap delegate = new ConcurrentHashMap();

    public Motable acquire(String id) {
        Motable motable = (Motable) delegate.get(id);
        if (null != motable) {
            try {
                getSharedLock(motable).acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            motable = (Motable) delegate.get(id);
        }
        return motable;
    }
    
    public Motable acquireExclusive(String id, long exclusiveSessionLockWaitTime) {
        Motable motable = (Motable) delegate.get(id);
        if (null != motable) {
            boolean success;
            try {
                success = getExclusiveLock(motable).attempt(exclusiveSessionLockWaitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            if (!success) {
                throw new MotableBusyException("Cannot obtain exclusive lock for Motable [" + id + "] after [" + 
                    exclusiveSessionLockWaitTime + "]ms.");
            }
            motable = (Motable) delegate.get(id);
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
        getSharedLock(motable).release();
    }

    public void releaseExclusive(Motable motable) {
        getExclusiveLock(motable).release();
    }
    
    public void remove(String name) {
        delegate.remove(name);
    }

    public int size() {
        return delegate.size();
    }
    
    protected Sync getSharedLock(Motable motable) {
        return motable.getReadWriteLock().readLock();
    }

    protected Sync getExclusiveLock(Motable motable) {
        return motable.getReadWriteLock().writeLock();
    }

}
