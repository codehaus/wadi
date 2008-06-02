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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.MotableBusyException;
import org.codehaus.wadi.core.contextualiser.EvictionStrategy;
import org.codehaus.wadi.core.motable.Motable;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractBestEffortEvicter extends AbstractEvicter {
    private static final Log log = LogFactory.getLog(AbstractBestEffortEvicter.class);

    protected final boolean strictOrdering;

    public AbstractBestEffortEvicter(int sweepInterval, boolean strictOrdering) {
        super(sweepInterval);
        this.strictOrdering = strictOrdering;
    }
    
    public void evict(ConcurrentMotableMap idToEvictable, EvictionStrategy evictionStrategy) {
        log.debug("Sweep started");

        List<Motable> toExpireList = new ArrayList<Motable>();
        List<Motable> toDemoteList = new ArrayList<Motable>();
        identifyDemotionsAndExpirations(idToEvictable, toExpireList, toDemoteList);
        
        Motable[] toExpire = toExpireList.toArray(new Motable[0]);
        Motable[] toDemote = toDemoteList.toArray(new Motable[0]);
        sortCandidates(toExpire, toDemote);
        
        expire(toExpire, evictionStrategy);
        demote(toDemote, evictionStrategy);

        if (log.isDebugEnabled()) {
            log.debug("Sweep completed - expirations=[" + toExpire.length + "], demotions=[" + toDemote.length + "]");
        }
    }

    protected void identifyDemotionsAndExpirations(ConcurrentMotableMap idToEvictable,
            List<Motable> toExpire,
            List<Motable> toDemote) {
        long time = System.currentTimeMillis();
        for (Iterator iter = idToEvictable.getNames().iterator(); iter.hasNext();) {
            String id = (String) iter.next();
            Motable motable = null;
            try {
                motable = idToEvictable.acquireExclusive(id, 1);
            } catch (MotableBusyException ignore) {
                // ignore
            }
            if (null == motable) {
                continue;
            }
            try {
                if (isReadyToExpire(motable, time)) {
                    toExpire.add(motable);
                    idToEvictable.remove(id);
                } else if (isReadyToDemote(motable, time)) {
                    toDemote.add(motable);
                    idToEvictable.remove(id);
                }
            } finally {
                idToEvictable.releaseExclusive(motable);
            }
        }
    }

    protected boolean isReadyToExpire(Motable motable, long time) {
        long ttl = motable.getTimeToLive(time);
        if (ttl <= 0) {
            return true;
        }
        return false;
    }

    protected boolean isReadyToDemote(Motable motable, long time) {
        long ttl = motable.getTimeToLive(time);
        if (testForDemotion(motable, time, ttl)) {
            return true;
        }
        return false;
    }

    protected void sortCandidates(Motable[] toExpire, Motable[] toDemote) {
        long time = System.currentTimeMillis();
        // if strict ordering is required we sort the candidate lists before the next stage.
        if (strictOrdering) {
            Comparator<Evictable> comparator = getComparator(time);
            Arrays.sort(toExpire, comparator);
            Arrays.sort(toDemote, comparator);
        }
    }

    protected void demote(Motable[] toDemote, EvictionStrategy evictionStrategy) {
        for (int i = 0; i < toDemote.length; i++) {
            Motable motable = toDemote[i];
            evictionStrategy.demote(motable);
        }
    }
    
    protected void expire(Motable[] toExpire, EvictionStrategy evictionStrategy) {
        for (int i = 0; i < toExpire.length; i++) {
            Motable motable = toExpire[i];
            evictionStrategy.expire(motable);
        }
    }

    protected Comparator<Evictable> getComparator(long time) {
        return new TimeToLiveComparator(time);
    }

    static class TimeToLiveComparator implements Comparator<Evictable> {
        protected final long _time;

        public TimeToLiveComparator(long time) {
            _time = time;
        }

        public boolean equals(Object obj) {
            return (this == obj)
                    || ((obj instanceof TimeToLiveComparator) && this._time == ((TimeToLiveComparator) obj)._time);
        }

        // orders smallest ttl first, largest last
        public int compare(Evictable o1, Evictable o2) {
            long delta = o1.getTimeToLive(_time) - o2.getTimeToLive(_time);
            if (delta < 0) {
                return -1;
            } else if (delta > 0) {
                return 1;
            }
            return 0;
        }

    }

}
