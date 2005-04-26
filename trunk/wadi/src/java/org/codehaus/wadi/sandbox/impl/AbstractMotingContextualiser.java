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
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Emoter;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Locker;
import org.codehaus.wadi.sandbox.Motable;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Abstract base for Contextualisers that are 'chained' - in other words - arranged in a single linked list
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractMotingContextualiser extends AbstractChainedContextualiser {
	protected final Log _log=LogFactory.getLog(getClass());

    protected final Locker _locker;
	protected final Evicter _evicter;

	public AbstractMotingContextualiser(Contextualiser next, Locker locker, Evicter evicter) {
		super(next);
        _locker=locker;
        _evicter=evicter;
	}

    public Sync getEvictionLock(String id, Motable motable) {
        return _locker.getLock(id, motable);
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
	public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync motionLock, boolean exclusiveOnly) throws IOException, ServletException {
	    return (handle(hreq, hres, chain, id, immoter, motionLock) ||
	            ((!(exclusiveOnly && !_next.isExclusive())) && _next.contextualise(hreq, hres, chain, id, getPromoter(immoter), motionLock, exclusiveOnly)));
	}

	public boolean promote(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync motionLock, Motable emotable) throws IOException, ServletException {
		Emoter emoter=getEmoter();
		Motable immotable=Utils.mote(emoter, immoter, emotable, id);
		if (immotable!=null) {
            return immoter.contextualise(hreq, hres, chain, id, immotable, motionLock);
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
    
    public Immoter getSharedDemoter() {
        if (isExclusive())
            return _next.getSharedDemoter();
        else
            return getImmoter();
    }

	public abstract void evict();

	public Evicter getEvicter(){return _evicter;}
	
	public abstract Motable get(String id);

    public boolean handle(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync motionLock) throws IOException, ServletException {
    	if (immoter!=null) {
    		Motable emotable=get(id);
    		return promote(hreq, hres, chain, id, immoter, motionLock, emotable); // motionLock should be released here...
    	} else
    		return false;
    }
    
    public void promoteToExclusive(Immoter immoter) {
        if (isExclusive())
            _next.promoteToExclusive(_next.isExclusive()?null:getImmoter());
        else {
            Emoter emoter=getEmoter();
            loadMotables(emoter, immoter);
            _next.promoteToExclusive(immoter);
        }
    }
}
