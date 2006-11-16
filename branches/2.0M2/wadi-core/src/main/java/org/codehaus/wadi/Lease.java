/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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
package org.codehaus.wadi;

import java.io.Serializable;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Lease - like a Sync, but locks are only granted for a given period after which they automatically unlock themselves.
 * This is an important construct in a distributed, HA system as, even in the case of failure, a locked resource will
 * be correctly unlocked.
 *
 * @author jules
 * @version $Revision$
 */
public interface Lease extends Sync {
    
    public interface Handle extends Serializable, Comparable {
    }
    
    /**
     * acquire a lease for a given period
     * 
     * @param leasePeriod the given period (in millis)
     * @return the handle of the lease acquired
     */
    Handle acquire(long leasePeriod) throws InterruptedException;
    
    /**
     * attempt the acquisition of a lease for a given period within a given timeframe
     * 
     * @param timeframe the timeframe within which to try to acquire the lease (in millis)
     * @param leasePeriod the period for which the lease is required (in millis)
     * @return the handle of the lease acquire or null in the case of failure
     * 
     * @throws InterruptedException
     */
    Handle attempt(long timeframe, long leasePeriod) throws InterruptedException;
    
    /**
     * A misleading name - 'release' as in 'unlock' the lease corresponding to the given handle
     * 
     * @param handle the handle of the Lease to be 'released'
     * 
     * @return whether or not the lease in question was still active at the point of 'release'
     */
    boolean release(Handle handle);
    
}
