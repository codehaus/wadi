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
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Promoter;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractChainedContextualiser implements Contextualiser {

	protected final Contextualiser _next;
	protected final Collapser _collapser;

	/**
	 *
	 */
	public AbstractChainedContextualiser(Contextualiser next, Collapser collapser) {
		super();
		_next=next;
		_collapser=collapser;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#contextualise(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Contextualiser)
	 */
	public boolean contextualise(ServletRequest req, ServletResponse res,
			FilterChain chain, String id, Promoter promoter, Sync promotionMutex, boolean localOnly) throws IOException, ServletException {
		boolean success=false;
		if ((success=contextualiseLocally(req, res, chain, id, promoter, promotionMutex))) {
			return success;
		} else if (!(localOnly && !_next.isLocal())) {
			boolean acquired=false;
			try {
				if (promotionMutex==null) {
					promotionMutex=_collapser.getLock(id);
					promotionMutex.acquire();
					acquired=true;
					// by the time we get the lock, another thread may have already promoted this context - try again locally...
					if ((success=contextualiseLocally(req, res, chain, id, promoter, promotionMutex))) {// mutex released here if successful
						return success;
					}
				}

				Promoter p=getPromoter(promoter);
				if ((success=_next.contextualise(req, res, chain, id, p, promotionMutex, localOnly))) // mutex released here if successful
					return success;

			} catch (InterruptedException e) {
				throw new ServletException("timed out collapsing requests for context: "+id, e);
			} finally {
				if (promotionMutex!=null && acquired && !success)
					promotionMutex.release();
			}
		}

		return success;
	}

	public abstract Promoter getPromoter(Promoter promoter);

	public abstract boolean contextualiseLocally(ServletRequest req, ServletResponse res,
			FilterChain chain, String id, Promoter promoter, Sync promotionMutex) throws IOException, ServletException;
}
