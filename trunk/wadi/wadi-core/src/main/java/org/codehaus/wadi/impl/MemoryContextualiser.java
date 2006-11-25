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

import java.util.Map;

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.PoolableInvocationWrapper;
import org.codehaus.wadi.PoolableInvocationWrapperPool;
import org.codehaus.wadi.Session;
import org.codehaus.wadi.SessionPool;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

/**
 * A Contextualiser that stores its state in Memory as Java Objects
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class MemoryContextualiser extends AbstractExclusiveContextualiser {
	private final SessionPool _pool;
    private final Immoter _immoter;
    private final Emoter _emoter;
    private final Emoter _evictionEmoter;
    private final PoolableInvocationWrapperPool _requestPool;

	public MemoryContextualiser(Contextualiser next, Evicter evicter, Map map, SessionPool pool, PoolableInvocationWrapperPool requestPool) {
		super(next, new RWLocker(), false, evicter, map);
        _pool = pool;
        _requestPool = requestPool;
        
        _immoter = new MemoryImmoter(_map);
        _emoter = new MemoryEmoter(_map);
        _evictionEmoter = new BaseMappedEmoter(_map);
    }

    protected boolean handleLocally(Invocation invocation, String id, Sync invocationLock, Motable motable) throws InvocationException {
        Sync stateLock = ((Session) motable).getSharedLock();
        try {
            try {
                Utils.acquireUninterrupted("State (shared)", id, stateLock);
            } catch (TimeoutException e) {
                _log.error("unexpected timeout - continuing without lock: " + id + " : " + stateLock, e);
                throw new WADIRuntimeException(e);
            }

            motable.setLastAccessedTime(System.currentTimeMillis());
            // we need a solution - MemoryContextualiser needs to separate Contexts and Motables cleanly...
            invocation.setSession((Session) motable); 
            if (!invocation.isProxiedInvocation()) {
                // part of the proxying proedure runs a null req...
                // restick clients whose session is here, but whose routing info
                // points elsewhere...
                _config.getRouter().reroute(invocation);
                // take wrapper from pool...
                PoolableInvocationWrapper wrapper = _requestPool.take();
                wrapper.init(invocation, (Session) motable);
                invocation.invoke(wrapper);
                wrapper.destroy();
                _requestPool.put(wrapper);
            } else {
                invocation.invoke();
            }
            return true;
        } finally {
            Utils.release("State (shared)", id, stateLock);
        }
    }

    class MemoryEmoter extends BaseMappedEmoter {

        public MemoryEmoter(Map map) {
            super(map);
        }

        public boolean emote(Motable emotable, Motable immotable) {
            String name = emotable.getName();
            Sync stateLock = ((Session) emotable).getExclusiveLock();
            try {
                Utils.acquireUninterrupted("State (excl.)", name, stateLock);
            } catch (TimeoutException e) {
                _log.error("unexpected timeout", e);
                return false;
            }
            try {
                return super.emote(emotable, immotable);
            } finally {
                Utils.release("State (excl.)", name, stateLock);
            }
        }

    }

    class MemoryImmoter extends AbstractMappedImmoter {

        public MemoryImmoter(Map map) {
            super(map);
        }

        public Motable newMotable() {
            return _pool.take();
        }

        public boolean contextualise(Invocation invocation, String id, Motable immotable, Sync motionLock)
                throws InvocationException {
            return handleLocally(invocation, id, motionLock, immotable);
        }

    }

    public Immoter getImmoter() {
        return _immoter;
    }

    public Emoter getEmoter() {
        return _emoter;
    }

    public Immoter getPromoter(Immoter immoter) {
        return immoter == null ? _immoter : immoter;
    }

    public Sync getEvictionLock(String id, Motable motable) {
        return ((Session) motable).getExclusiveLock();
    }

    public Emoter getEvictionEmoter() {
        return _evictionEmoter;
    }

    public void expire(Motable motable) {
        _config.expire(motable);
    }

}
