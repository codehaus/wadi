/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Context;
import org.codehaus.wadi.sandbox.context.ContextPool;
import org.codehaus.wadi.sandbox.context.Contextualiser;
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
	public MemoryContextualiser(Contextualiser next, Map map, ContextPool pool) {
		super(next, map);
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
		
		public void promoteAndContextualise(ServletRequest req, ServletResponse res, FilterChain chain, String id, Context context, Sync overlap)
		throws IOException, ServletException {
			try {
				// TODO - revisit and think about unrolling on exception...
				Sync shared=context.getSharedLock();
				shared.acquire(); // now this is locked into the container until we use/release it
				_map.put(id, context);
				overlap.release(); // now available to other 'loading' threads
				contextualise(req, res, chain, id);
				shared.release();
			} catch (InterruptedException e) {
				throw new ServletException("problem promoting context: "+id, e);
			}
		}
		
		public Context nextContext() {
			return _pool.take();
		}
	}

	protected Promoter _promoter=new MemoryPromoter();
	public Promoter getPromoter(Promoter promoter) {return _promoter;}
}
