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

/**
 * 
 * @version $Revision: 1538 $
 */
public interface ConcurrentMotableMap {
    void put(Object id, Motable motable);

    void remove(Object id);

    Motable get(Object id);

    Motable acquire(Object id);

    Motable acquireExclusive(Object id, long exclusiveSessionLockWaitTime) throws MotableBusyException;

    void release(Motable motable);

    void releaseExclusive(Motable motable);

    int size();

    boolean isEmpty();

    Set<Object> getIds();
}
