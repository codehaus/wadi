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
import org.codehaus.wadi.sandbox.PoolableHttpServletRequestWrapper;
import org.codehaus.wadi.sandbox.HttpServletRequestWrapperPool;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

/**
 * A Contextualiser that stores its state in Memory as Java Objects
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
    protected final HttpServletRequestWrapperPool _requestPool;

	public MemoryContextualiser(Contextualiser next, Evicter evicter, Map map, StreamingStrategy streamer, ContextPool pool, HttpServletRequestWrapperPool requestPool) {
		super(next, evicter, map);
		_pool=pool;
		
		// TODO - streamer should be used inside Motables get/setBytes() methods  but that means a ref in every session :-(
		_streamer=streamer;

		_immoter=new MemoryImmoter(_map);
		_emoter=new MemoryEmoter(_map);
		_evictionEmoter=new AbstractMappedEmoter(_map){public String getInfo(){return "memory";}};
        
        _requestPool=requestPool;
	}

	public boolean isLocal(){return true;}

	// TODO - sometime figure out how to make this a wrapper around AbstractMappedContextualiser.handle() instead of a replacement...
	public boolean handle(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock) throws IOException, ServletException {
	    Motable emotable=get(id);
	    if (emotable==null)
	        return false; // we cannot proceed without the session...

	    if (immoter!=null) {
	        return promote(hreq, hres, chain, id, immoter, promotionLock, emotable); // promotionLock will be released here...
	    } else {
	        return contextualiseLocally(hreq, hres, chain, id, promotionLock, emotable);
	    }
	}

	public boolean contextualiseLocally(HttpServletRequest req, HttpServletResponse res, FilterChain chain, String id, Sync promotionLock, Motable motable)  throws IOException, ServletException {
	    Sync lock=((Context)motable).getSharedLock();
	    boolean acquired=false;
	    try{
	        try {
	            Utils.acquireUninterrupted(lock);
	            acquired=true;
	        } catch (TimeoutException e) {
	            _log.error("unexpected timeout - continuing without lock: "+id, e);
	            // give this some more thought - TODO
	        }
	        
	        if (promotionLock!=null) promotionLock.release();
	        
	        if (motable.getInvalidated()) {
	            _log.trace("context disappeared whilst we were waiting for lock: "+id);
	        }
	        
            // take wrapper from pool...
            motable.setLastAccessedTime(System.currentTimeMillis());
            PoolableHttpServletRequestWrapper wrapper=_requestPool.take();
            wrapper.init(req, (Context)motable);
	        chain.doFilter(wrapper, res);
            wrapper.destroy();
            _requestPool.put(wrapper);
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
	        try {
	            Utils.acquireUninterrupted(lock);
	        } catch (TimeoutException e) {
	            _log.error("unexpected timeout", e);
	            return false;
	        }
	        
	        if (emotable.getInvalidated())
	            return false; // we lost race to motable and it has gone...
	        
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
	        // motable has just been promoted and promotionLock released, so we
	        // pass in a null promotionLock...
	        contextualiseLocally(hreq, hres, chain, id, null, immotable);
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
