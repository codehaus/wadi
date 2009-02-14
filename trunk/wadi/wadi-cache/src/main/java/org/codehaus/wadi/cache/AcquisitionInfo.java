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

package org.codehaus.wadi.cache;


/**
 *
 * @version $Rev:$ $Date:$
 */
public class AcquisitionInfo {
    public static final AcquisitionInfo DEFAULT = new AcquisitionInfo(20000, 4000, false, false);
    public static final AcquisitionInfo EXCLUSIVE_LOCAL_INFO = new AcquisitionInfo(1, 1, false, false);
    
    private final long cacheEntryAccessWaitTime; 
    private final long exclusiveLockWaitTime;
    protected final boolean pin;
    protected final boolean unpin;
    
    public AcquisitionInfo(long cacheEntryAccessWaitTime, long exclusiveLockWaitTime, boolean pin, boolean unpin) {
        if (1 > cacheEntryAccessWaitTime) {
            throw new IllegalArgumentException("cacheEntryAccessWaitTime must be greater than 0");
        } else if (1 > exclusiveLockWaitTime) {
            throw new IllegalArgumentException("exclusiveLockWaitTime must be greater than 0");
        } else if (pin && unpin) {
            throw new IllegalArgumentException("only pin or unpin can be true");
        }
        this.cacheEntryAccessWaitTime = cacheEntryAccessWaitTime;
        this.exclusiveLockWaitTime = exclusiveLockWaitTime;
        this.pin = pin;
        this.unpin = unpin;
    }

    public AcquisitionInfo(AcquisitionInfo prototype, boolean pin, boolean unpin) {
        this(prototype.cacheEntryAccessWaitTime, prototype.exclusiveLockWaitTime, pin, unpin);
    }
    
    public long getCacheEntryAccessWaitTime() {
        return cacheEntryAccessWaitTime;   
    }

    public long getExclusiveLockWaitTime() {
        return exclusiveLockWaitTime;
    }
    
    public boolean isPin() {
        return pin;
    }

    public boolean isUnpin() {
        return unpin;
    }

    @Override
    public String toString() {
        return "AcquisitionInfo [cacheEntryAccessWaitTime=" + cacheEntryAccessWaitTime + "; exclusiveLockWaitTime="
                + exclusiveLockWaitTime + "]";
    }
}
