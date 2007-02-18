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
	
	public boolean contextualise(Invocation invocation, String key, Immoter immoter, boolean exclusiveOnly) throws InvocationException {
        boolean handled = handle(invocation, key, immoter, exclusiveOnly);
        if (handled) {
            return true;
        }
        return next.contextualise(invocation, key, getPromoter(immoter), exclusiveOnly);
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

    protected abstract Motable get(String id, boolean exclusiveOnly);

    protected boolean handle(Invocation invocation, String id, Immoter immoter, boolean exclusiveOnly) throws InvocationException {
		if (null != immoter) {
            Motable emotable = get(id, exclusiveOnly);
            if (null != emotable) {
                return promote(invocation, id, immoter, emotable);
            }
        }
        return false;
	}

    protected boolean promote(Invocation invocation, String id, Immoter immoter, Motable emotable) throws InvocationException {
        Emoter emoter = getEmoter();
        Motable immotable = Utils.mote(emoter, immoter, emotable, id);
        if (immotable != null) {
            return immoter.contextualise(invocation, id, immotable);
        } else {
            return false;
        }
    }
    
}
