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
import org.codehaus.wadi.sandbox.Collapser;
import org.codehaus.wadi.sandbox.Context;
import org.codehaus.wadi.sandbox.ContextPool;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Emoter;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Motable;

import EDU.oswego.cs.dl.util.concurrent.NullSync;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

/**
 * A Contextualiser that stores it's state in Memory as Java Objects
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class MemoryContextualiser extends AbstractMappedContextualiser {
	protected final Log _log = LogFactory.getLog(getClass());
	protected final ContextPool _pool;
	protected final StreamingStrategy _streamer;
	protected final Immoter _immoter;
	protected final Emoter _emoter;

	public MemoryContextualiser(Contextualiser next, Collapser collapser, Map map, Evicter evicter, StreamingStrategy streamer, ContextPool pool) {
		super(next, collapser, map, evicter);
		_pool=pool;
		_streamer=streamer;

		_immoter=new MemoryImmoter();
		_emoter=new MemoryEmoter();
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

			if (immoter!=null)
				return promote(hreq, hres, chain, id, immoter, promotionLock, emotable);

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
		long time=System.currentTimeMillis();
		for (Iterator i=_map.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e=(Map.Entry)i.next();
			String id=(String)e.getKey();
			Motable emotable=(Motable)e.getValue();
			if (_evicter.evict(id, emotable, time)) { // first test without lock - cheap
				Sync lock=((Context)emotable).getExclusiveLock();
				boolean acquired=false;
				// this should be a while loop
				try {
					if (lock.attempt(0) && _evicter.evict(id, emotable, time)) { // then confirm with exclusive lock
						acquired=true;
						Immoter immoter=_next.getDemoter(id, emotable);
						Emoter emoter=getEmoter();
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
	        //_log.info("removal (memory): "+id);
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
	    
	    public void commit(String id, Motable immotable) {
	        _map.put(id, immotable); // assumes that Map does own syncing...
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

	public Immoter getImmoter(){return _immoter;}
	public Emoter getEmoter(){return _emoter;}

	public Immoter getPromoter(Immoter immoter) {
		return immoter==null?_immoter:immoter;
	}
}
