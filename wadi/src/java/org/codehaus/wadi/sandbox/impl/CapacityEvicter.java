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

import java.util.Comparator;

import org.codehaus.wadi.sandbox.Evictable;

/**
 * Evict based on an intended capacity. In other words, if the number of sessions grows over
 * a parameterised limit, reduce it until it falls within it. Since strictOrdering is forced,
 * and a Comparattor is supplied, sessions will be evicted according to their lastAccessedTime.
 * Those that have been inactive for the most time will be evicted first.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class CapacityEvicter extends AbstractBestEffortEvicter {

    // TODO - although this will work perfectly, it is a little inefficient, since it will include
    // all sessions in its sweep - can we optimise this ? Perhaps an abstract method to decide
    // whether to sweep at all ?
    // how about only testing true, if inactivity reaches a certain level aswell ?
    
    // a further optimisation could be made ; as you traverse the session list you remember the
    // smallest ttl that did not result in eviction. When you go back to sleep, it is for
    // max(this-figure, normal-sleep-interval). This prevents waking up when there is nothing
    // to do. If, in the interim, a session is created or has its maxinactivetime changed so as
    // to have a ttl below the one you have recorded, you have to adjust the wake up time accordingly...
    
    static class InactivityComparator implements Comparator {

        public boolean equals(Object obj) {
            return (this==obj) || (obj instanceof InactivityComparator);
        }

        // orders largest lat first, smallest last
        public int compare(Object o1, Object o2) {
            return (int)(((Evictable)o2).getLastAccessedTime()-((Evictable)o1).getLastAccessedTime());
        }
        
    }
    
    protected final Comparator _comparator=new InactivityComparator();
    
    public Comparator getComparator(long time) {
        return _comparator;
    }
    
    protected final int _capacity;
    
    public CapacityEvicter(int sweepInterval, int capacity) {
        super(sweepInterval, true);
        _capacity=capacity;
    }

    public boolean test(Evictable evictable, long time, long ttl) {
        return _config.getMap().size()>_capacity;
    }

}
