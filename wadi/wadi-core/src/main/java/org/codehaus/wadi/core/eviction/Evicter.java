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

import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.contextualiser.EvictionStrategy;
import org.codehaus.wadi.core.motable.Motable;

/**
 * An API for deciding whether or not to evict a given Evictable
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface Evicter {
    void schedule(Timer timer, TimerTask timerTask);

    void cancel(TimerTask timerTask);
    
    void evict(ConcurrentMotableMap idToEvictable, EvictionStrategy evictionStrategy);
    
    boolean testForDemotion(Motable motable, long time, long ttl);
}
