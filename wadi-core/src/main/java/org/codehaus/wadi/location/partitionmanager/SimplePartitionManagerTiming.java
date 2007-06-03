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
package org.codehaus.wadi.location.partitionmanager;


/**
 * 
 * @version $Revision: 1538 $
 */
public class SimplePartitionManagerTiming {
	private static final int WAIT_DEFINED_PARTITION_MANAGER = 60 * 1000;
	private static final long PARTITION_UPDATE_WAIT_TIME = 5000;
	private static final long PARTITION_EVACUATION_WAIT_TIME = 5000;
	private static final long EVACUATION_BACKOFF_TIME = 1000;
	private static final long SESSION_RELOCATION_WAIT_TIME = 1000;
	private static final long REPOPULATION_WAIT_TIME = 3000;

    private int waitForBootTime =  WAIT_DEFINED_PARTITION_MANAGER;
    private long waitForPartitionUpdateTime = PARTITION_UPDATE_WAIT_TIME;
    private long waitForEvacuationTime = PARTITION_EVACUATION_WAIT_TIME;
    private long evacuationBackoffTime = EVACUATION_BACKOFF_TIME;
    private long sessionRelocationWaitTime = SESSION_RELOCATION_WAIT_TIME;
    private long waitForRepopulationTime = REPOPULATION_WAIT_TIME;
    
    public int getWaitForBootTime() {
        return waitForBootTime;
    }
    
    public void setWaitForBootTime(int waitForBootTime) {
        if (1 > waitForBootTime) {
            throw new IllegalArgumentException("waitForBootTime must be > 0");
        }
        this.waitForBootTime = waitForBootTime;
    }
    
    public long getWaitForEvacuationTime() {
        return waitForEvacuationTime;
    }
    
    public void setWaitForEvacuationTime(long waitForEvacuationTime) {
        if (1 > waitForEvacuationTime) {
            throw new IllegalArgumentException("waitForEvacuationTime must be > 0");
        }
        this.waitForEvacuationTime = waitForEvacuationTime;
    }
    
    public long getWaitForPartitionUpdateTime() {
        return waitForPartitionUpdateTime;
    }
    
    public void setWaitForPartitionUpdateTime(long waitForPartitionUpdateTime) {
        if (1 > waitForPartitionUpdateTime) {
            throw new IllegalArgumentException("waitForPartitionUpdateTime must be > 0");
        }
        this.waitForPartitionUpdateTime = waitForPartitionUpdateTime;
    }

    public long getEvacuationBackoffTime() {
        return evacuationBackoffTime;
    }

    public void setEvacuationBackoffTime(long evacuationBackoffTime) {
        if (1 > evacuationBackoffTime) {
            throw new IllegalArgumentException("evacuationBackoffTime must be > 0");
        }
        this.evacuationBackoffTime = evacuationBackoffTime;
    }

    public long getSessionRelocationWaitTime() {
        return sessionRelocationWaitTime;
    }

    public long getSessionRelocationWaitTimeForRelocater() {
        return (long) (sessionRelocationWaitTime * 1.5);
    }
    
    public void setSessionRelocationWaitTime(long sessionRelocationWaitTime) {
        if (1 > sessionRelocationWaitTime) {
            throw new IllegalArgumentException("sessionRelocationWaitTime must be > 0");
        }
        this.sessionRelocationWaitTime = sessionRelocationWaitTime;
    }
    
    public long getWaitForRepopulationTime() {
        return waitForRepopulationTime;
    }

    public void setWaitForRepopulationTime(long waitForRepopulationTime) {
        if (1 > waitForRepopulationTime) {
            throw new IllegalArgumentException("waitForRepopulationTime must be > 0");
        }
        this.waitForRepopulationTime = waitForRepopulationTime;
    }
    
}
