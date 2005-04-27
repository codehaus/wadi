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
package org.codehaus.wadi.sandbox.impl;

import org.codehaus.wadi.sandbox.Evictable;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.EvicterConfig;

public abstract class AbstractBestEffortEvicter extends AbstractEvicter {

    // BestEffort Evicters pay no attention to these notifications - to do so would be very expensive.
    // If you plan to build e.g. an LRU Evicter, by ordering Sessions according to time-to-live, use
    // this notification to reorder your Sessions after each change/access...
    
    public void insert(Evictable evictable) {/* do nothing */}
    public void remove(Evictable evictable) {/* do nothing */}
    public void setLastAccessedTime(Evictable evictable, long oldTime, long newTime) {/* do nothing */}
    public void setMaxInactiveInterval(Evictable evictable, int oldInterval, int newInterval) {/* do nothing */}

}
