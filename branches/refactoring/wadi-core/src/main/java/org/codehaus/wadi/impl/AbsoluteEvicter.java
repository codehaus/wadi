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

import org.codehaus.wadi.Evictable;

/**
 * An Evicter which evicts Evictables if they have timed out or after an absolute period of inactivity.
 * For example, using this Evicter, you could evict sessions after 30 minutes of inactivity.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class AbsoluteEvicter extends AbstractBestEffortEvicter {
    protected final long _inactiveInterval;
    
    public AbsoluteEvicter(int sweepInterval, boolean strictOrdering, int inactivityInterval) {
        super(sweepInterval, strictOrdering);
        _inactiveInterval = inactivityInterval * 1000;
    }

    public boolean test(Evictable evictable, long time, long ttl) {
        return time - evictable.getLastAccessedTime() >= _inactiveInterval;
    }
    
}
