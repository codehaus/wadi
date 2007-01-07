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
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Abstract base for Contextualisers that are 'chained' - in other words - arranged in a single linked list
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public abstract class AbstractMotingContextualiser extends AbstractChainedContextualiser {
	
	public AbstractMotingContextualiser(Contextualiser next) {
		super(next);
	}
	
	public boolean contextualise(Invocation invocation, String key, Immoter immoter, Sync invocationLock, boolean exclusiveOnly) throws InvocationException {
        boolean handled = handle(invocation, key, immoter, invocationLock, exclusiveOnly);
        if (handled) {
            return true;
        } else if (exclusiveOnly && !next.isExclusive()) {
            return false;
        }
        return next.contextualise(invocation, key, getPromoter(immoter), invocationLock, exclusiveOnly);
	}
	
    public void promoteToExclusive(Immoter immoter) {
        if (isExclusive()) {
            next.promoteToExclusive(next.isExclusive() ? null : getImmoter());
        } else {
            Emoter emoter = getEmoter();
            load(emoter, immoter);
            next.promoteToExclusive(immoter);
        }
    }
	
	public Immoter getSharedDemoter() {
		if (isExclusive()) {
		    return next.getSharedDemoter();
        } else {
            return getImmoter();
        }
	}
	
    /**
     * @return - an Emoter that facilitates removal of Motables from this Contextualiser's own store
     */
    protected abstract Emoter getEmoter();
    
    /**
     * @return - an Immoter that facilitates insertion of Motables into this Contextualiser's own store
     */
    protected abstract Immoter getImmoter();
    
    protected Immoter getPromoter(Immoter immoter) {
        // just pass contexts straight through...
        return immoter; 
    }

    protected abstract Motable acquire(String id, boolean exclusiveOnly);

    protected abstract void release(Motable motable, boolean exclusiveOnly);
	
    protected boolean handle(Invocation invocation, String id, Immoter immoter, Sync motionLock, boolean exclusiveOnly) throws InvocationException {
		if (null != immoter) {
            Motable emotable = acquire(id, exclusiveOnly);
            if (null != emotable) {
                try {
                    return promote(invocation, id, immoter, motionLock, emotable);
                } finally {
                    release(emotable, exclusiveOnly);
                }
            }
        }
        return false;
	}

    protected boolean promote(Invocation invocation, String id, Immoter immoter, Sync motionLock, Motable emotable) throws InvocationException {
        Emoter emoter = getEmoter();
        Motable immotable = Utils.mote(emoter, immoter, emotable, id);
        if (immotable != null) {
            return immoter.contextualise(invocation, id, immotable, motionLock);
        } else {
            return false;
        }
    }
    
}