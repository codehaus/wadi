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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Evictable;
import org.codehaus.wadi.EvictionStrategy;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.core.ConcurrentMotableMap;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractBestEffortEvicter extends AbstractEvicter {
    protected final Log _log = LogFactory.getLog(getClass());
    protected final boolean _strictOrdering;

    public AbstractBestEffortEvicter(int sweepInterval, boolean strictOrdering) {
        super(sweepInterval);
        _strictOrdering = strictOrdering;
    }
    
    public void evict(ConcurrentMotableMap idToEvictable, EvictionStrategy evictionStrategy) {
        _log.trace("sweep started");

        // get candidates
        long time = System.currentTimeMillis();

        // perform a cheap first pass, remembering those candidates
        // which look as if they should be expired/demoted...
        List toExpireList = new ArrayList();
        List toDemoteList = new ArrayList();

        idToEvictable.acquireAll();
        
        int expirations;
        int demotions;
        try {
            scanForExpireAndDemoteCandidates(idToEvictable, time, toExpireList, toDemoteList);
            
            Object[] toExpire = toExpireList.toArray();
            Object[] toDemote = toDemoteList.toArray();
            sortCandidates(time, toExpire, toDemote);
            
            expirations = expire(time, toExpire, evictionStrategy);
            demotions = demote(time, toDemote, evictionStrategy);
        } finally {
            idToEvictable.releaseAll();
        }

        if (_log.isDebugEnabled()) {
            _log.debug("sweep completed (" + (System.currentTimeMillis() - time) + " millis) - expirations:"
                    + expirations + ", demotions:" + demotions);
        }
    }

    private void sortCandidates(long time, Object[] toExpire, Object[] toDemote) {
        // if strict ordering is required we sort the candidate lists
        // before the next stage.
        if (_strictOrdering) {
            Comparator comparator = getComparator(time);
            Arrays.sort(toExpire, comparator);
            Arrays.sort(toDemote, comparator);
        }
    }

    protected void scanForExpireAndDemoteCandidates(ConcurrentMotableMap idToMotable, long time, List toExpireList, List toDemoteList) {
        for (Iterator i = idToMotable.getMotables().iterator(); i.hasNext();) {
            // no locking on this pass...
            Evictable e = (Evictable) i.next();
            long ttl = e.getTimeToLive(time);
            if (ttl < 0) {
                // if ttl==0, we do NOT expire - this avoids any problems with
                // sessions that are being recycled...
                toExpireList.add(e);
            } else if (test(e, time, ttl)) {
                toDemoteList.add(e);
            }
        }
    }

    protected int demote(long time, Object[] toDemote, EvictionStrategy evictionStrategy) {
        int demotions = 0;
        int l = toDemote.length;
        for (int i = 0; i < l; i++) {
            Motable motable = (Motable) toDemote[i];
            evictionStrategy.demote(motable);
            demotions++;
        }
        return demotions;
    }
    
    protected int expire(long time, Object[] toExpire, EvictionStrategy evictionStrategy) {
        int expirations = 0;
        int l = toExpire.length;
        for (int i = 0; i < l; i++) {
            Motable motable = (Motable) toExpire[i];
            evictionStrategy.expire(motable);
            expirations++;
        }
        return expirations;
    }

    protected Comparator getComparator(long time) {
        return new TimeToLiveComparator(time);
    }

    static class TimeToLiveComparator implements Comparator {
        protected final long _time;

        public TimeToLiveComparator(long time) {
            _time = time;
        }

        public boolean equals(Object obj) {
            return (this == obj)
                    || ((obj instanceof TimeToLiveComparator) && this._time == ((TimeToLiveComparator) obj)._time);
        }

        // orders smallest ttl first, largest last
        public int compare(Object o1, Object o2) {
            return (int) (((Evictable) o1).getTimeToLive(_time) - ((Evictable) o2).getTimeToLive(_time));
        }

    }

}
