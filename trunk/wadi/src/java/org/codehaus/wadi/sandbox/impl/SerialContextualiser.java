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
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Motable;

import EDU.oswego.cs.dl.util.concurrent.NullSync;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

/**
 * Ensure that any Contualisations that pass through are serialised according to the strategy imposed by our Collapser.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class SerialContextualiser implements Contextualiser {
	protected static final Log _log = LogFactory.getLog(AbstractImmoter.class);
	
	protected final Contextualiser _next;
	protected final Collapser _collapser;
	protected final Evicter _evicter=new NeverEvicter();
	protected final Sync _dummyLock=new NullSync();
	
	public SerialContextualiser(Contextualiser next, Collapser collapser) {
		_next=next;
		_collapser=collapser;
	}
	
	public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock, boolean localOnly) throws IOException, ServletException {
		Sync lock=_collapser.getLock(id);
		boolean acquired=false;
		boolean released=false;
		try {
			do {
				try {
					lock.acquire();
					acquired=true;
				} catch (TimeoutException e) {
					_log.error("unexpected timeout - proceding without lock", e);
				} catch (InterruptedException e) {
					_log.warn("unexpected interruption", e);
				}
			} while (Thread.interrupted());
			
			// lock is to be released as soon as context is available to subsequent contextualisations...
			return (released=_next.contextualise(hreq, hres, chain, id, immoter, acquired?lock:_dummyLock, localOnly));
		} finally {
			if (acquired && !released) lock.release();
		}
	}
	
	public void evict() {}
	public Evicter getEvicter() {return _evicter;}
	public boolean isLocal() {return _next.isLocal();}
	public Immoter getDemoter(String id, Motable motable) {return _next.getDemoter(id, motable);}
}
