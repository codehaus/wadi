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
import java.util.Map;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Evictable;
import org.codehaus.wadi.EvicterConfig;
import org.codehaus.wadi.Motable;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public abstract class AbstractBestEffortEvicter extends AbstractEvicter {

    static class TimeToLiveComparator implements Comparator {

        protected final long _time;

        public TimeToLiveComparator(long time) {
            _time=time;
        }

        public boolean equals(Object obj) {
            return (this==obj) || ((obj instanceof TimeToLiveComparator) && this._time==((TimeToLiveComparator)obj)._time);
        }

        // orders smallest ttl first, largest last
        public int compare(Object o1, Object o2) {
            return (int)(((Evictable)o1).getTimeToLive(_time)-((Evictable)o2).getTimeToLive(_time));
        }

    }

    public Comparator getComparator(long time) {
        return new TimeToLiveComparator(time);
    }

    class BestEffortTask extends TimerTask {

        public void run() {
            evict();
        }
    }

    protected final Log _log=LogFactory.getLog(getClass());
    protected final int _sweepInterval;
    protected final boolean _strictOrdering;
    protected TimerTask _task;

    protected EvicterConfig _config;

    public AbstractBestEffortEvicter(int sweepInterval, boolean strictOrdering) {
        _sweepInterval=sweepInterval;
        _strictOrdering=strictOrdering;
    }

    public void init(EvicterConfig config) {
        super.init(config);
        _config=config;
    }

    public void start() throws Exception {
        if (_log.isTraceEnabled()) _log.trace("starting (sweep interval: "+_sweepInterval+" sec[s])");
        long interval=_sweepInterval*1000;
        _config.getTimer().schedule((_task=new BestEffortTask()), interval, interval);
    }

    public void stop() throws Exception {
        _task.cancel();
	_log.trace("stopped");
    }

    public void destroy() {
        _config=null;
    }

    public void evict() {
      _log.trace("sweep started");

        RankedRWLock.setPriority(RankedRWLock.EVICTION_PRIORITY); // TODO - shouldn't really be here, but...

        // get candidates
        long time=System.currentTimeMillis();
        Map candidates=_config.getMap();

        // perform a cheap first pass, remembering those candidates
        // which look as if they should be expired/demoted...
        List toExpireList=new ArrayList();
        List toDemoteList=new ArrayList();
        for (Iterator i=candidates.values().iterator(); i.hasNext(); ) { // TODO - Map MUST support snapshot iteration - investigate
            // no locking on this pass...
            Evictable e=(Evictable)i.next();
            long ttl=e.getTimeToLive(time);
            if (ttl<0) // if ttl==0, we do NOT expire - this avoids any problems with sessions that are being recycled...
                toExpireList.add(e);
            else if (test(e, time, ttl))
                toDemoteList.add(e);
        }

        Object[] toExpire=toExpireList.toArray();
        toExpireList=null;
        Object[] toDemote=toDemoteList.toArray();
        toDemoteList=null;

        // if strict ordering is required we sort the candidate lists
        // before the next stage.
        if (_strictOrdering) {
            Comparator comparator=getComparator(time);
            Arrays.sort(toDemote, comparator);
            Arrays.sort(toDemote, comparator);
            comparator=null;
        }

        // perform a more expensive second pass, retesting each of the
        // remembered candidates within a lock. If the test proves positive
        // they will be expired/demoted within this same lock. Thus a session
        // will never be expired/demoted, whilst there is a request pertinent
        // to it within the container, as these two events are mutually exclusive.
        // If we cannot acquire the lock immediately, we assume that a request has
        // taken it during the intervening period, and ignore this candidate.

        // deal with expirations...
        int expirations=0;
        {
	  int l=toExpire.length;
	  for (int i=0; i<l; i++) {
	    Motable motable=(Motable)toExpire[i]; // TODO - not happy about an Evicter knowing about Motables
	    String id=motable.getName();
	    if (id!=null) {
	      Sync sync=_config.getEvictionLock(id, motable);
	      if (Utils.attemptUninterrupted(sync)) {
		if (motable.getTimedOut(time)) {
		  _config.expire(motable);
		  expirations++;
		}
		sync.release();
	      } else {
		if (_log.isTraceEnabled()) _log.trace("could not acquire expiration lock: "+id);
	      }
	    }
	  }
	  toExpire=null;
        }

        // and the same again for demotions...
        int demotions=0;
        {
            int l=toDemote.length;
            for (int i=0; i<l; i++) {
                Motable motable=(Motable)toDemote[i]; // TODO - not happy about an Evicter knowing about Motables
                String id=motable.getName();
                Sync sync=_config.getEvictionLock(id, motable);
                if (Utils.attemptUninterrupted(sync)) {
                    if (test(motable, time, motable.getTimeToLive(time))) { // IDEA - could have remembered ttl
                        _config.demote(motable);
                        demotions++;
                    }
                    sync.release();
                } else {
                    if (_log.isTraceEnabled()) _log.trace("could not acquire demotion lock: "+id);
                }
            }
            toDemote=null;
        }

        RankedRWLock.setPriority(RankedRWLock.NO_PRIORITY); // TODO - shouldn't really be here, but...

        if (_log.isDebugEnabled()) _log.debug("sweep completed ("+(System.currentTimeMillis()-time)+" millis) - expirations:"+expirations+", demotions:"+demotions);
    }

    // BestEffort Evicters pay no attention to these notifications - to do so would be very expensive.
    // If you plan to build e.g. an LRU Evicter, by ordering Sessions according to time-to-live, use
    // this notification to reorder your Sessions after each change/access...

    public void insert(Evictable evictable) {/* do nothing */}
    public void remove(Evictable evictable) {/* do nothing */}
    public void setLastAccessedTime(Evictable evictable, long oldTime, long newTime) {/* do nothing */}
    public void setMaxInactiveInterval(Evictable evictable, int oldInterval, int newInterval) {/* do nothing */}

}
