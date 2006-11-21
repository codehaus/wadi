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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.EvicterConfig;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Locker;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.PartitionMapper;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Basic implementation for Contextualisers which maintain a local Map of references
 * to Motables.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractExclusiveContextualiser extends AbstractMotingContextualiser implements EvicterConfig {
	protected final Map _map;
    protected final Evicter _evicter;

    public AbstractExclusiveContextualiser(Contextualiser next, Locker locker, boolean clean, Evicter evicter, Map map) {
        super(next, locker, clean);
        if (null == map) {
            throw new IllegalArgumentException("map is required");
        } else if (null == evicter) {
            throw new IllegalArgumentException("evicter is required");
        }
        _map = map;
        _evicter = evicter;
    }

    public Motable get(String id) {
        return (Motable) _map.get(id);
    }

    // TODO - sometime figure out how to make this a wrapper around
    // AbstractChainedContextualiser.handle() instead of a replacement...
    public boolean handle(Invocation invocation, String id, Immoter immoter, Sync motionLock)
            throws InvocationException {
        Motable emotable = get(id);
        if (emotable == null) {
            return false;
        } else if (immoter != null) {
            return promote(invocation, id, immoter, motionLock, emotable);
        } else {
            return false;
        }
    }

    public Emoter getEvictionEmoter() {
        return getEmoter();
    }

    protected void unload() {
        Emoter emoter = getEmoter();

        // emote all our Motables using it
        RankedRWLock.setPriority(RankedRWLock.EVICTION_PRIORITY);

        int i = 0;
        for (Iterator iter = _map.values().iterator(); iter.hasNext();) {
            Motable emotable = (Motable) iter.next();
            try {
                String name = emotable.getName();
                if (name != null) {
                    Immoter immoter = _next.getSharedDemoter();
                    Utils.mote(emoter, immoter, emotable, name);
                    i++;
                }
            } catch (Exception e) {
                _log.warn("unexpected problem while unloading session", e);
            }
        }

        RankedRWLock.setPriority(RankedRWLock.NO_PRIORITY);
        _log.info("unloaded sessions: " + i);
    }

    public void init(ContextualiserConfig config) {
        super.init(config);
        _evicter.init(this);
    }

    public void start() throws Exception {
        super.start();
        _evicter.start();
    }

    public void stop() throws Exception {
        unload();
        _evicter.stop();
        super.stop();
    }

    public void load(Emoter emoter, Immoter immoter) {
        // MappedContextualisers are all Exclusive
    }

    public Evicter getEvicter() {
        return _evicter;
    }

    public Immoter getDemoter(String name, Motable motable) {
        long time = System.currentTimeMillis();
        if (getEvicter().test(motable, time, motable.getTimeToLive(time))) {
            return _next.getDemoter(name, motable);
        } else {
            return getImmoter();
        }
    }

    public int getSize() {
        return _map.size();
    }

    public Map getMap() {
        return _map;
    }

    public Timer getTimer() {
        return _config.getTimer();
    }

    public Sync getEvictionLock(String id, Motable motable) {
        return _locker.getLock(id, motable);
    }

    public void demote(Motable emotable) {
        String id = emotable.getName();
        if (id == null) {
            // we lost a race...
            return;
        }
        Immoter immoter = _next.getDemoter(id, emotable);
        Emoter emoter = getEvictionEmoter();
        Utils.mote(emoter, immoter, emotable, id);
    }

    public int getMaxInactiveInterval() {
        return _config.getMaxInactiveInterval();
    }

    public void findRelevantSessionNames(PartitionMapper mapper, Map keyToSessionNames) {
        super.findRelevantSessionNames(mapper, keyToSessionNames);
        // TODO - this is broken as the API does not explicitly tell us about the synchronization policy of _map.
        for (Iterator i = _map.keySet().iterator(); i.hasNext();) {
            String name = (String) i.next();
            int key = mapper.map(name);
            Collection sessionNames = (Collection) keyToSessionNames.get(new Integer(key));
            if (null != sessionNames) {
                sessionNames.add(name);
            }
        }
    }

}
