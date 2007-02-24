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
package org.codehaus.wadi.core.eviction;

import java.util.Timer;
import java.util.TimerTask;

import org.codehaus.wadi.Evicter;

/**
 * Abstract base for Evicters.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractEvicter implements Evicter {
    private final int sweepInterval;
    
    public AbstractEvicter(int sweepInterval) {
        if (1 > sweepInterval) {
            throw new IllegalArgumentException("sweepInterval must be > 0");
        }
        this.sweepInterval = sweepInterval * 1000;
    }
    
    public void schedule(Timer timer, TimerTask timerTask) {
        timer.schedule(timerTask, sweepInterval, sweepInterval);
    }

    public void cancel(TimerTask timerTask) {
        timerTask.cancel();
    }
    
}