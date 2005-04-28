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

public class DummyEvicter extends AbstractEvicter {

    public void evict() {/* do nothing */}
    public boolean test(Evictable evictable, long time, long ttl) {return false;}
    
    public void setLastAccessedTime(Evictable evictable, long oldTime, long newTime) {/* do nothing */}
    public void setMaxInactiveInterval(Evictable evictable, int oldInterval,int newInterval) {/* do nothing */}
    public void insert(Evictable evictable) {/* do nothing */}
    public void remove(Evictable evictable) {/* do nothing */}

}
