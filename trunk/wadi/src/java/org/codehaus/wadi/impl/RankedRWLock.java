/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.impl;

// maybe collapse with superclass when sandbox is merged into mainline.

/**
 * A ReadWriteLock with prioritisable writer threads. The set of priority ranks is tailored
 * to WADI. 
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class RankedRWLock extends org.codehaus.wadi.old.RWLock {
    
    public final static int INVALIDATION_PRIORITY=5; // explicit invalidation by user
    public final static int EMMIGRATION_PRIORITY=4; // session is required on another node
    public final static int EVICTION_PRIORITY=3; // session is being evicted (implicit timeout)
    public final static int IMMIGRATION_PRIORITY=2; // TODO - do we need this ?
    public final static int CREATION_PRIORITY=1; // TODO - do we need this ?
    public final static int NO_PRIORITY=0; // used to remove any of the above from a thread...
    
    protected final static int MAX_PRIORITY=INVALIDATION_PRIORITY;
    
    public RankedRWLock() {
        super(MAX_PRIORITY);
    }

}
