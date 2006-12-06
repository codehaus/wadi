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

import java.util.Collection;
import java.util.Set;

import org.codehaus.wadi.Motable;
import org.codehaus.wadi.impl.WADIRuntimeException;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @version $Revision: 1538 $
 */
public class OswegoConcurrentSessionMap implements ConcurrentMotableMap {
    private final ConcurrentHashMap delegate = new ConcurrentHashMap();

    public Motable acquire(String id) {
        Motable motable = (Motable) delegate.get(id);
        if (null != motable) {
            try {
                motable.getLock().acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new WADIRuntimeException(e);
            }
        }
        return motable;
    }

    public void acquireAll() {
    }

    public Collection getMotables() {
        return delegate.values();
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
        motable.getLock().release();
    }

    public void releaseAll() {
    }

    public void remove(String name) {
        delegate.remove(name);
    }

    public int size() {
        return delegate.size();
    }
    
}
