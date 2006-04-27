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

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Locker;
import org.codehaus.wadi.Motable;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Abstract base for Contextualisers that are 'chained' - in other words - arranged in a single linked list
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractMotingContextualiser extends AbstractChainedContextualiser {
	
	protected final Locker _locker;
	
	protected ContextualiserConfig _config;
	
	protected final boolean _clean;
	
	public AbstractMotingContextualiser(Contextualiser next, Locker locker, boolean clean) {
		super(next);
		_locker=locker;
		_clean=clean;
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
	public boolean contextualise(Invocation invocation, String id, Immoter immoter, Sync motionLock, boolean exclusiveOnly) throws InvocationException {
		return (handle(invocation, id, immoter, motionLock) ||
				((!(exclusiveOnly && !_next.isExclusive())) && _next.contextualise(invocation, id, getPromoter(immoter), motionLock, exclusiveOnly)));
	}
	
	// TODO - I don't think that we need test isExclusive anymore - or even need this flag...
	
	public boolean promote(Invocation invocation, String id, Immoter immoter, Sync motionLock, Motable emotable) throws InvocationException {
		Emoter emoter=getEmoter();
		Motable immotable=Utils.mote(emoter, immoter, emotable, id);
		if (immotable!=null) {
			return immoter.contextualise(invocation, id, immotable, motionLock);
		} else {
			return false;
		}
	}
	
	public Immoter getPromoter(Immoter immoter) {
		return immoter; // just pass contexts straight through...
	}
	
	public Immoter getSharedDemoter() {
		if (isExclusive())
			return _next.getSharedDemoter();
		else
			return getImmoter();
	}
	
	public abstract Motable get(String id);
	
	public boolean handle(Invocation invocation, String id, Immoter immoter, Sync motionLock) throws InvocationException {
		if (immoter!=null) {
			Motable emotable=get(id);
			return promote(invocation, id, immoter, motionLock, emotable); // motionLock should be released here...
		} else
			return false;
	}
	
	public void promoteToExclusive(Immoter immoter) {
		if (isExclusive())
			_next.promoteToExclusive(_next.isExclusive()?null:getImmoter());
		else {
			Emoter emoter=getEmoter();
			load(emoter, immoter);
			_next.promoteToExclusive(immoter);
		}
	}
	
	public void init(ContextualiserConfig config) {
		super.init(config);
		_config=config;
	}
	
	public void destroy() {
		_config=null;
		super.destroy();
	}
	
}
