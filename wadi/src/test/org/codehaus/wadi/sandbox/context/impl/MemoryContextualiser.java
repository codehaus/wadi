/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
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
package org.codehaus.wadi.sandbox.context.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.StreamingStrategy;
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Context;
import org.codehaus.wadi.sandbox.context.ContextPool;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Emoter;
import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Immoter;
import org.codehaus.wadi.sandbox.context.Motable;

import EDU.oswego.cs.dl.util.concurrent.NullSync;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class MemoryContextualiser extends AbstractMappedContextualiser {
	protected final Log _log = LogFactory.getLog(getClass());
	protected final ContextPool _pool;
	protected final StreamingStrategy _streamer;

	public MemoryContextualiser(Contextualiser next, Collapser collapser, Map map, Evicter evicter, StreamingStrategy streamer, ContextPool pool) {
		super(next, collapser, map, evicter);
		_pool=pool;
		_streamer=streamer;
		
		_emoter=new MemoryEmoter(); // overwrite - yeugh ! - fix when we have a LifeCycle
	}

	public boolean isLocal(){return true;}

	public boolean contextualiseLocally(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock) throws IOException, ServletException {
		Motable emotable=(Motable)_map.get(id);
		if (emotable==null)
			return false; // we cannot proceed without the session...
		
		Sync shared=((Context)emotable).getSharedLock();
		boolean acquired=false;
		try {
			shared.acquire();
			acquired=true;
			// now that we know the Context has been promoted to this point and is going nowhere we can allow other threads that were trying to find it proceed...
			
			if (promotionLock!=null)
				promotionLock.release();
			
			if (immoter!=null)
				return contextualiseElsewhere(hreq, hres, chain, id, immoter, promotionLock, emotable);
			
			return contextualiseLocally(hreq, hres, chain, id, promotionLock, emotable);
			
		} catch (InterruptedException e) {
			throw new ServletException("timed out acquiring context", e); // TODO - good idea ?
		} finally {
			if (acquired)
				shared.release(); // should we release here ?
		}
	}

	// TODO - c.f. AbstractMappedContextualiser.evict()
	public void evict() {
		// TODO - lock the map - move expensive stuff out of lock...
		for (Iterator i=_map.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e=(Map.Entry)i.next();
			String id=(String)e.getKey();
			Motable emotable=(Motable)e.getValue();
			if (_evicter.evict(id, emotable)) { // first test without lock - cheap
				Sync lock=((Context)emotable).getExclusiveLock();
				boolean acquired=false;
				// this should be a while loop
				try {
					if (lock.attempt(0) && _evicter.evict(id, emotable)) { // then confirm with exclusive lock
						acquired=true;
						Immoter immoter=_next.getDemoter(id, emotable);
						Emoter emoter=getEmoter();
						_log.info("demoting : "+emotable);
						Utils.mote(emoter, immoter, emotable, id);
					}
				} catch (InterruptedException ie) {
					// should be done in a loop...
					_log.warn("unexpected interruption to eviction - ignoring", ie);
				} finally {
					if (acquired)
						lock.release();
				}
			}
		}
	}

	public boolean contextualiseLocally(HttpServletRequest req, HttpServletResponse res, FilterChain chain, String id, Sync promotionLock, Motable motable)  throws IOException, ServletException {
		_log.info("contextualising: "+id);
		chain.doFilter(req, res);
		return true;
	}

	// TODO - merge with MappedEmoter soon
	class MemoryEmoter implements Emoter {
		
		public boolean prepare(String id, Motable emotable, Motable immotable) {
			// TODO - acquire exclusive lock
			_map.remove(id);
			return true;
		}
		
		public void commit(String id, Motable emotable) {
			emotable.tidy();
			_log.info("removal (memory): "+id);
			// TODO - release exclusive lock
		}
		
		public void rollback(String id, Motable emotable) {
			_map.put(id, emotable);
			// TODO - locking ?
		}
		
		public String getInfo() {
			return "memory";
		}		
	}
	
	class MemoryImmoter extends AbstractImmoter {
		
		public Motable nextMotable(String id, Motable emotable) {
			return _pool.take();
		}
		
		
		// TODO - I don't think this is ever called...
		public boolean prepare(String id, Motable motable, Sync lock) {
			do {
				try {
					// TODO - we only want this lock if we can guarantee that we will also contextualise...
					lock.acquire();
					// TODO - we should ensure that session is still valid
					return super.prepare(id, motable, null);
				} catch (TimeoutException e) {
					_log.error("could not acquire shared lock: "+id);
					return false;
				} catch (InterruptedException e) {
					_log.debug("interrupted whilst trying for shared lock: "+id, e);
					// go around again
				}
			} while (Thread.interrupted());
			
			_log.error("THIS CODE SHOULD NOT BE EXECUTED");
			return false; // keep Eclipse compiler happy
		}
		
		public void commit(String id, Motable immotable) {
			_map.put(id, immotable); // assumes that Map does own syncing...
			_log.info("insertion (memory): "+id);
		}
		
		public void contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Motable immotable) throws IOException, ServletException {
			Context context=(Context)immotable;
			try {
				MemoryContextualiser.this.contextualiseLocally(hreq, hres, chain, id, new NullSync(), immotable); // TODO - promotionLock ?
			} finally {
				context.getSharedLock().release();
			}
		}
		
		public String getInfo() {
			return "memory";
		}
	}

	protected Immoter _immoter=new MemoryImmoter();
	
	public Immoter getPromoter(Immoter immoter) {
		// do NOT pass through, we want to promote sessions into this store
		return _immoter;
		}

	public Immoter getDemoter(String id, Motable motable) {
		if (_evicter.evict(id, motable)) {
			return _next.getDemoter(id, motable);
		} else {
			return _immoter;
		}
	}
}
