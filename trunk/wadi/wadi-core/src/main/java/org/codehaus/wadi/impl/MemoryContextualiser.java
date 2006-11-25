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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.codehaus.wadi.Streamer;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

/**
 * A Contextualiser that stores its state in Memory as Java Objects
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class MemoryContextualiser extends AbstractExclusiveContextualiser {
	protected final SessionPool _pool;
	protected final Streamer _streamer;
	protected final Immoter _immoter;
	protected final Emoter _emoter;
	protected final Emoter _evictionEmoter;
	protected final PoolableInvocationWrapperPool _requestPool;
	protected final Log _lockLog=LogFactory.getLog("org.codehaus.wadi.LOCKS");

	public MemoryContextualiser(Contextualiser next, Evicter evicter, Map map, Streamer streamer, SessionPool pool, PoolableInvocationWrapperPool requestPool) {
		super(next, new RWLocker(), false, evicter, map);
		_pool=pool;

		// TODO - streamer should be used inside Motables get/setBytes() methods  but that means a ref in every session :-(
		_streamer=streamer;

		_immoter=new MemoryImmoter(_map);
		_emoter=new MemoryEmoter(_map);
		_evictionEmoter=new BaseMappedEmoter(_map);

		_requestPool=requestPool;
	}

	public boolean isExclusive(){return true;}

	// TODO - sometime figure out how to make this a wrapper around AbstractMappedContextualiser.handle() instead of a replacement...
	public boolean handle(Invocation invocation, String id, Immoter immoter, Sync motionLock) throws InvocationException {
		Motable emotable=get(id);
		if (emotable==null) {
		    return false;
        } else if (immoter!=null) {
			return promote(invocation, id, immoter, motionLock, emotable);
		} else {
			return contextualiseLocally(invocation, id, motionLock, emotable);
		}
	}

	public boolean contextualiseLocally(Invocation invocation, String id, Sync invocationLock, Motable motable)  throws InvocationException {
		Sync stateLock=((Session)motable).getSharedLock();
		try {
			try {
				Utils.acquireUninterrupted("State (shared)", id, stateLock);
			} catch (TimeoutException e) {
				_log.error("unexpected timeout - continuing without lock: "+id+" : "+stateLock, e);
                throw new WADIRuntimeException(e);
			}

			motable.setLastAccessedTime(System.currentTimeMillis());
            invocation.setSession((Session)motable);  // we need a solution - MemoryContextualiser needs to separate Contexts and Motables cleanly...
			if (false == invocation.isProxiedInvocation()) {
				// part of the proxying proedure runs a null req...
				// restick clients whose session is here, but whose routing info points elsewhere...
				_config.getRouter().reroute(invocation); // TODO - hmm... still thinking
				// take wrapper from pool...
				PoolableInvocationWrapper wrapper = _requestPool.take();
				wrapper.init(invocation, (Session)motable);
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

		public boolean contextualise(Invocation invocation, String id, Motable immotable, Sync motionLock) throws InvocationException {
			return contextualiseLocally(invocation, id, motionLock, immotable);
		}

	}

	public Immoter getImmoter(){return _immoter;}
	public Emoter getEmoter(){return _emoter;}
	public Immoter getPromoter(Immoter immoter) {return immoter==null?_immoter:immoter;}

	public Sync getEvictionLock(String id, Motable motable){return ((Session)motable).getExclusiveLock();}
	public Emoter getEvictionEmoter(){return _evictionEmoter;} // leave lock-taking to evict()...

	public void expire(Motable motable) {_config.expire(motable);}
}
