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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Emoter;
import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Immoter;
import org.codehaus.wadi.sandbox.context.Motable;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractChainedContextualiser implements Contextualiser {
	protected final Log _log=LogFactory.getLog(getClass());

	protected final Contextualiser _next;
	protected final Collapser _collapser;
	protected final Evicter _evicter;

	public AbstractChainedContextualiser(Contextualiser next, Collapser collapser, Evicter evicter) {
		super();
		_next=next;
		_collapser=collapser;
		_evicter=evicter;
	}
	
	/**
	 * @return - an Emoter that facilitates removal of Motables from this Contextualiser's own store
	 */
	public abstract Emoter getEmoter();
	
	/**
	 * @return - an Immoter that facilitates insertion of Motables into this Contextualiser's own store
	 */
	public abstract Immoter getImmoter();

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#contextualise(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Contextualiser)
	 */
	public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock, boolean localOnly) throws IOException, ServletException {
		boolean success=false;
		if (true==(success=contextualiseLocally(hreq, hres, chain, id, immoter, promotionLock))) {
			return success;
		} else if (!(localOnly && !_next.isLocal())) {
			boolean acquired=false;
			try {
				if (promotionLock==null) {
					promotionLock=_collapser.getLock(id);
					promotionLock.acquire();
					acquired=true;
					// by the time we get the lock, another thread may have already promoted this context - try again locally...
					if (true==(success=contextualiseLocally(hreq, hres, chain, id, immoter, promotionLock))) {// mutex released here if successful
						return success;
					}
				}

				Immoter p=getPromoter(immoter);
				if (true==(success=_next.contextualise(hreq, hres, chain, id, p, promotionLock, localOnly))) // mutex released here if successful
					return success;

			} catch (InterruptedException e) {
				throw new ServletException("timed out collapsing requests for context: "+id, e);
			} finally {
				if (promotionLock!=null && acquired && !success)
					promotionLock.release();
			}
		}

		return success;
	}

	public abstract boolean contextualiseLocally(HttpServletRequest hreq, HttpServletResponse hres,
			FilterChain chain, String id, Immoter immoter, Sync promotionLock) throws IOException, ServletException;

	public boolean contextualiseElsewhere(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock, Motable emotable) throws IOException, ServletException {
		Emoter emoter=getEmoter();
		Motable immotable=Utils.mote(emoter, immoter, emotable, id);
		if (immotable!=null) {
			promotionLock.release();
			immoter.contextualise(hreq, hres, chain, id, immotable);
			return true;
		} else {
			return false;
		}
	}

	public Immoter getPromoter(Immoter immoter) {
		return immoter; // just pass contexts straight through...
	}

	public Immoter getDemoter(String id, Motable motable) {
		if (getEvicter().evict(id, motable))
			return _next.getDemoter(id, motable);
		else	
			return getImmoter();
	}

	public abstract void evict();
	
	public Evicter getEvicter(){return _evicter;}
}
