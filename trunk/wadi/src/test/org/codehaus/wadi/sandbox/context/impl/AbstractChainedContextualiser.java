/*
 * Created on Feb 16, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Contextualiser;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author jules
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class AbstractChainedContextualiser implements Contextualiser {

	protected final Contextualiser _next;

	/**
	 * 
	 */
	public AbstractChainedContextualiser(Collapser collapser, Contextualiser next) {
		super();
		_collapser=collapser;
		_next=next;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#contextualise(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Contextualiser)
	 */
	public boolean contextualise(ServletRequest req, ServletResponse res,
			FilterChain chain, String id, Contextualiser previous) throws IOException, ServletException {
		if (contextualiseLocally(req, res, chain, id, previous))
			return true;
		else {
			Sync lock=_collapser.getLock(id);
			try {
				lock.acquire();
				// by the time we get the lock, another thread may have already promoted this context - try again locally...
				if (contextualiseLocally(req, res, chain, id, previous))
					return true;
				else
					return _next.contextualise(req, res, chain, id, previous);
			} catch (InterruptedException e) {
				throw new ServletException("timed out collapsing of request for session: "+id, e);
			} finally {
				lock.release();
			}
		}
	}
	
	public abstract boolean contextualiseLocally(ServletRequest req, ServletResponse res,
			FilterChain chain, String id, Contextualiser previous) throws IOException, ServletException;

	protected final Collapser _collapser;
}
