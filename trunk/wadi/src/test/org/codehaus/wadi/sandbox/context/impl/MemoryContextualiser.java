/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Context;
import org.codehaus.wadi.sandbox.context.ContextPool;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Motable;
import org.codehaus.wadi.sandbox.context.Promoter;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MemoryContextualiser extends AbstractMappedContextualiser {
	protected final Log _log = LogFactory.getLog(getClass());
	protected final ContextPool _pool;
	
	/**
	 * 
	 */
	public MemoryContextualiser(Contextualiser next, Collapser collapser, Map map, Evicter evicter, ContextPool pool) {
		super(next, collapser, map, evicter);
		_pool=pool;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#contextualise(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Contextualiser)
	 */
	public boolean contextualiseLocally(ServletRequest req, ServletResponse res,
			FilterChain chain, String id, Promoter promoter, Sync promotionMutex) throws IOException, ServletException {
		Context c=(Context)_map.get(id);
		if (c==null) {
			return false;
		} else {
			Sync shared=c.getSharedLock();
			try {
				shared.acquire();
				// now that we know the Context has been promoted to this point and is going nowhere we can allow other threads that were trying to find it proceed...
				if (promotionMutex!=null)
					promotionMutex.release();
				contextualise(req, res, chain, id);
			} catch (InterruptedException e) {
				throw new ServletException("timed out acquiring context", e);
			} finally {
				shared.release(); // should we release here ?
			}
			return true;
		}
	}
	
	protected void contextualise(ServletRequest req, ServletResponse res, FilterChain chain, String id)  throws IOException, ServletException {
		_log.info("contextualising: "+id);
		chain.doFilter(req, res);
	}
	
	class MemoryPromoter implements Promoter {

		public Context nextContext() {
			return _pool.take();
		}
		
		public boolean prepare(String id, Context context) {
			try {
				context.getSharedLock().acquire();
				_log.info("promoting (to memory): "+id);
				_log.info("insert (memory): "+id);
				_map.put(id, context);
				return true;
			} catch (InterruptedException e) {
				_log.warn("promotion abandoned: "+id, e);
				return false;
			}			
		}
		
		public void commit(String id, Context context) {
			}
		
		public void rollback(String id, Context context) {
			_log.info("remove (memory): "+id);
			_map.remove(id);
		}
		
		public void contextualise(ServletRequest req, ServletResponse res, FilterChain chain, String id, Context context) throws IOException, ServletException {
			try {
				MemoryContextualiser.this.contextualise(req, res, chain, id);
			} finally {
			context.getSharedLock().release();
			}
		}
	}

	protected Promoter _promoter=new MemoryPromoter();
	public Promoter getPromoter(Promoter promoter) {return _promoter;}
	
	public void demote(String key, Motable val) {
		if (_evicter.evict(key, val)) {
			_next.demote(key, val);
		} else {
			_log.info("insert (memory): "+key);
			_map.put(key, val);
		}
	}
	
	public void evict() {
		for (Iterator i=_map.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e=(Map.Entry)i.next();
			String key=(String)e.getKey();
			Context val=(Context)e.getValue();
			if (_evicter.evict(key, val)) { // first test without lock - cheap
				Sync exclusive=val.getExclusiveLock();
				try {
					if (exclusive.attempt(0) && _evicter.evict(key, val)) { // then confirm with exclusive lock
						// do we need the promotion lock ? don't think so - TODO
						_log.info("demoting (from memory): "+key);
						_next.demote(key, val);
						i.remove();
						_log.info("remove (memory): "+key);
						exclusive.release();
					}
				} catch (InterruptedException ie) {
					_log.warn("unexpected interruption to eviction - ignoring", ie);
				}
			}
		}
	}
}
