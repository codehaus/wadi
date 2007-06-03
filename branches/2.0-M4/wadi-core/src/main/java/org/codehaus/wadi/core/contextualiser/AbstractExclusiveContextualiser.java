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
package org.codehaus.wadi.core.contextualiser;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.eviction.Evicter;
import org.codehaus.wadi.core.motable.Emoter;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.util.Utils;
import org.codehaus.wadi.location.partitionmanager.PartitionMapper;

/**
 * Basic implementation for Contextualisers which maintain a local Map of references
 * to Motables.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractExclusiveContextualiser extends AbstractMotingContextualiser {
    private static Log log = LogFactory.getLog(AbstractExclusiveContextualiser.class);
    
	protected final ConcurrentMotableMap map;
    private final Evicter evicter;
    private final Timer timer;
    private TimerTask evictionTask;

    public AbstractExclusiveContextualiser(Contextualiser next, Evicter evicter, ConcurrentMotableMap map) {
        super(next);
        if (null == map) {
            throw new IllegalArgumentException("map is required");
        } else if (null == evicter) {
            throw new IllegalArgumentException("evicter is required");
        }
        this.map = map;
        this.evicter = evicter;
        
        timer = new Timer();
    }

    public void promoteToExclusive(Immoter immoter) {
        next.promoteToExclusive(getImmoter());
    }
    
    public Immoter getSharedDemoter() {
        return next.getSharedDemoter();
    }
    
    public Set getSessionNames() {
        return map.getNames();
    }
    
    protected Motable get(String id, boolean exclusiveOnly) {
        throw new UnsupportedOperationException();
    }
    
    public Motable acquire(String id, boolean exclusiveOnly) {
        if (exclusiveOnly) {
            return map.acquireExclusive(id);
        } else {
            return map.acquire(id);
        }
    }

    protected void release(Motable motable, boolean exclusiveOnly) {
        if (exclusiveOnly) {
            map.releaseExclusive(motable);
        } else {
            map.release(motable);
        }
    }
    
    public final boolean handle(Invocation invocation,
            String id,
            Immoter immoter,
            boolean exclusiveOnly) throws InvocationException {
        Motable emotable = acquire(id, exclusiveOnly);
        if (emotable == null) {
            return false;
        }
        try {
            if (null != immoter) {
                return promote(invocation, id, immoter, emotable);
            } else {
                return handleLocally(invocation, id, emotable);
            }
        } finally {
            release(emotable, exclusiveOnly);
        }
    }

    protected void doStart() throws Exception {
        evictionTask = new TimerTask() {
            public void run() {
                evicter.evict(map, new BasicEvictionStrategy());
            }
        };
        evicter.schedule(timer, evictionTask);
    }

    protected void doStop() throws Exception {
        unload();
        evicter.cancel(evictionTask);
    }

    public Immoter getDemoter(String name, Motable motable) {
        long time = System.currentTimeMillis();
        if (evicter.testForDemotion(motable, time, motable.getTimeToLive(time))) {
            return next.getDemoter(name, motable);
        } else {
            return getImmoter();
        }
    }

    protected void doFindRelevantSessionNames(PartitionMapper mapper, Map keyToSessionNames) {
        for (Iterator i = map.getNames().iterator(); i.hasNext();) {
            String name = (String) i.next();
            int key = mapper.map(name);
            Collection sessionNames = (Collection) keyToSessionNames.get(new Integer(key));
            if (null != sessionNames) {
                sessionNames.add(name);
            }
        }
    }

    protected boolean handleLocally(Invocation invocation, String id, Motable motable) throws InvocationException {
        return false;
    }

    protected void unload() {
        Emoter emoter = getEmoter();

        int i = 0;
        for (Iterator iter = map.getNames().iterator(); iter.hasNext();) {
            String id = (String) iter.next();
            Motable motable = map.acquireExclusive(id);
            if (null == motable) {
                continue;
            }
            try {
                try {
                    Immoter immoter = next.getSharedDemoter();
                    Utils.mote(emoter, immoter, motable, id);
                    i++;
                } catch (Exception e) {
                    log.warn("unexpected problem while unloading session", e);
                }
                map.remove(id);
            } finally {
                map.releaseExclusive(motable);
            }
        }
        log.info("Unloaded sessions=[" + i + "]");
    }

    protected class BasicEvictionStrategy implements EvictionStrategy {

        public void demote(Motable motable) {
            String id = motable.getName();
            Immoter immoter = next.getDemoter(id, motable);
            Emoter emoter = getEmoter();
            Utils.mote(emoter, immoter, motable, id);
        }

        public void expire(Motable motable) {
            try {
                motable.destroy();
            } catch (Exception e) {
                log.warn("Problem while destroying motable", e);
            }
        }
        
    }
    
}
