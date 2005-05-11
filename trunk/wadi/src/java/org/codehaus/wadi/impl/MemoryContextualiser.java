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

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.wadi.Context;
import org.codehaus.wadi.ContextPool;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Evictable;
import org.codehaus.wadi.Evicter;
import org.codehaus.wadi.HttpServletRequestWrapperPool;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.PoolableHttpServletRequestWrapper;
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
	protected final ContextPool _pool;
	protected final Streamer _streamer;
	protected final Immoter _immoter;
	protected final Emoter _emoter;
	protected final Emoter _evictionEmoter;
    protected final HttpServletRequestWrapperPool _requestPool;

	public MemoryContextualiser(Contextualiser next, Evicter evicter, Map map, Streamer streamer, ContextPool pool, HttpServletRequestWrapperPool requestPool) {
		super(next, new RWLocker(), false, evicter, map);
		_pool=pool;

		// TODO - streamer should be used inside Motables get/setBytes() methods  but that means a ref in every session :-(
		_streamer=streamer;

		_immoter=new MemoryImmoter(_map);
		_emoter=new MemoryEmoter(_map);
		_evictionEmoter=new AbstractMappedEmoter(_map){public String getInfo(){return "memory";}};

        _requestPool=requestPool;
	}

	public boolean isExclusive(){return true;}

	// TODO - sometime figure out how to make this a wrapper around AbstractMappedContextualiser.handle() instead of a replacement...
	public boolean handle(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync motionLock) throws IOException, ServletException {
	    Motable emotable=get(id);
	    if (emotable==null)
	        return false; // we cannot proceed without the session...

	    if (immoter!=null) {
	        return promote(hreq, hres, chain, id, immoter, motionLock, emotable); // motionLock will be released here...
	    } else {
	        return contextualiseLocally(hreq, hres, chain, id, motionLock, emotable);
	    }
	}

	public boolean contextualiseLocally(HttpServletRequest req, HttpServletResponse res, FilterChain chain, String id, Sync motionLock, Motable motable)  throws IOException, ServletException {
	    Sync lock=((Context)motable).getSharedLock();
	    boolean acquired=false;
	    try{
	        try {
	            Utils.acquireUninterrupted(lock);
	            acquired=true;
	        } catch (TimeoutException e) {
	            if (_log.isErrorEnabled()) _log.error("unexpected timeout - continuing without lock: "+id, e);
	            // give this some more thought - TODO
	        }

	        if (motionLock!=null) motionLock.release();

	        if (motable.getName()==null) {
	            if (_log.isTraceEnabled()) _log.trace("context disappeared whilst we were waiting for lock: "+id);
	        }

            motable.setLastAccessedTime(System.currentTimeMillis());
            if (req!=null) { // part of the proxying proedure runs a null req...
                // restick clients whose session is here, but whose routing info points elsewhere...
                _config.getRouter().reroute(req, res); // TODO - hmm... still thinking
                // take wrapper from pool...
                PoolableHttpServletRequestWrapper wrapper=_requestPool.take();
                wrapper.init(req, (Context)motable);
                chain.doFilter(wrapper, res);
                wrapper.destroy();
                _requestPool.put(wrapper);
            } else {
                chain.doFilter(req, res);
            }
	        return true;
	    } finally {
	        if (acquired) lock.release();
	    }
	}

    class MemoryEmoter extends AbstractMappedEmoter {

        public MemoryEmoter(Map map) {
            super(map);
        }

        public boolean prepare(String name, Motable emotable, Motable immotable) {
            Sync lock=((Context)emotable).getExclusiveLock();
            try {
                Utils.acquireUninterrupted(lock);
            } catch (TimeoutException e) {
                _log.error("unexpected timeout", e);
                return false;
            }

            if (emotable.getName()==null)
                return false; // we lost race to motable and it has gone...

            return super.prepare(name, emotable, immotable);
        }

        public void commit(String name, Motable emotable) {
            super.commit(name, emotable);
            ((Context)emotable).getExclusiveLock().release();
        }

        public void rollback(String name, Motable emotable) {
            super.rollback(name, emotable);
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

	    public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Motable immotable, Sync motionLock) throws IOException, ServletException {
            return contextualiseLocally(hreq, hres, chain, id, motionLock, immotable);
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

    public void setLastAccessTime(Evictable evictable, long oldTime, long newTime) {_evicter.setLastAccessedTime(evictable, oldTime, newTime);}
    public void setMaxInactiveInterval(Evictable evictable, int oldInterval, int newInterval) {_evicter.setMaxInactiveInterval(evictable, oldInterval, newInterval);}

    public void expire(Motable motable) {_config.expire(motable);}

}
