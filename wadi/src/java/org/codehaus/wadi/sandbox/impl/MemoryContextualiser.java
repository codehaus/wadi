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
	protected final Emoter _evictionEmoter;

	public MemoryContextualiser(Contextualiser next, Map map, Evicter evicter, StreamingStrategy streamer, ContextPool pool) {
		super(next, map, evicter);
		_pool=pool;
		_streamer=streamer;

		_immoter=new MemoryImmoter(_map);
		_emoter=new MemoryEmoter(_map);
		_evictionEmoter=new AbstractMappedEmoter(_map){public String getInfo(){return "memory";}};
	}

	public boolean isLocal(){return true;}

	public boolean contextualiseLocally(HttpServletRequest req, HttpServletResponse res, FilterChain chain, String id, Sync promotionLock, Motable motable)  throws IOException, ServletException {
	    Sync lock=((Context)motable).getSharedLock();
	    boolean acquired=false;
	    try{
	        do {
	            try {
	                lock.acquire();
	                acquired=true;
	            } catch (TimeoutException e) {
	                _log.error("unexpected timeout - continuing without lock", e);
	            } catch (InterruptedException e) {
	                _log.warn("unexpected interruption - continuing", e);
	            }
	        } while (Thread.interrupted());
	        
	        chain.doFilter(req, res);
	        return true;
	    } finally {
	        if (acquired) lock.release();
	    }
	}
	
	class MemoryEmoter extends AbstractMappedEmoter {
	    
	    public MemoryEmoter(Map map) {
	        super(map);
	    }
	    
	    public boolean prepare(String id, Motable emotable, Motable immotable) {
	        Sync lock=((Context)emotable).getExclusiveLock();
	        do {
	            try {
	                lock.acquire();
	            } catch (TimeoutException e) {
	                _log.error("unexpected timeout", e);
	                return false;
	            } catch (InterruptedException e) {
	                _log.warn("unexpected interruption - continuing", e);
	            }
	        } while (Thread.interrupted());
	        
	        return super.prepare(id, emotable, immotable);
	    }
	    
	    public void commit(String id, Motable emotable) {
	        super.commit(id, emotable);
	        ((Context)emotable).getExclusiveLock().release();
	    }
	    
	    public void rollback(String id, Motable emotable) {
	        super.rollback(id, emotable);
	        ((Context)emotable).getExclusiveLock().release();
	    }
	    
	    public String getInfo() {
	        return "memory";
	    }
	}
	
	class MemoryImmoter extends AbstractMappedImmoter {
	    
	    public MemoryImmoter(Map map) {
	        super(map);
	    }
	    
	    public Motable nextMotable(String id, Motable emotable) {
	        return _pool.take();
	    }
	    
	    public void contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Motable immotable) throws IOException, ServletException {
	        contextualiseLocally(hreq, hres, chain, id, new NullSync(), immotable); // TODO - promotionLock ?
	    }
	    
	    public String getInfo() {
	        return "memory";
	    }
	}

	public Immoter getImmoter(){return _immoter;}
	public Emoter getEmoter(){return _emoter;}
	public Immoter getPromoter(Immoter immoter) {return immoter==null?_immoter:immoter;}
	
	public Sync getEvictionLock(String id, Motable motable){return ((Context)motable).getExclusiveLock();}
	public Emoter getEvictionEmoter(){return _evictionEmoter;} // leave lock-taking to evict()...
}
