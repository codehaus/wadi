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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.Collapser;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Emoter;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Motable;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Abstract base for Contextualisers that are 'chained' - in other words - arranged in a single linked list
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
	    return (contextualiseLocally(hreq, hres, chain, id, immoter, promotionLock) ||
	            ((!(localOnly && !_next.isLocal())) && _next.contextualise(hreq, hres, chain, id, getPromoter(immoter), promotionLock, localOnly)));
	}

	public abstract boolean contextualiseLocally(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock) throws IOException, ServletException;

	public boolean promote(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock, Motable emotable) throws IOException, ServletException {
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
