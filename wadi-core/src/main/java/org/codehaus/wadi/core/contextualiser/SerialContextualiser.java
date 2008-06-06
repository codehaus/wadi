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
package org.codehaus.wadi.core.contextualiser;

import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.ConcurrentMotableMap;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;



/**
 * Ensure that any Contextualisations that pass through are serialised according to the strategy imposed by our Collapser.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SerialContextualiser extends AbstractDelegatingContextualiser {
    private static final Log log = LogFactory.getLog(SerialContextualiser.class);
    
	private final Collapser collapser;
    private final ConcurrentMotableMap map;

    public SerialContextualiser(Contextualiser next, Collapser collapser, ConcurrentMotableMap map) {
        super(next);
        if (null == collapser) {
            throw new IllegalArgumentException("collapser is required");
        } else if (null == map) {
            throw new IllegalArgumentException("map is required");
        }
        this.collapser = collapser;
        this.map = map;
    }

    public boolean contextualise(Invocation invocation,
            String key,
            Immoter immoter,
            boolean exclusiveOnly) throws InvocationException {
        Lock invocationLock = collapser.getLock(key);

        // the promotion begins here. allocate a lock and continue...
        try {
            invocationLock.lockInterruptibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InvocationException(e);
        } catch (Exception e) {
            log.error("could not acquire session within timeframe: " + key);
            return false;
        }

        try {
            // whilst we were waiting for the motionLock, the session in question may have been moved back into memory 
            // somehow. before we proceed, confirm that this has not happened.
            Motable context = map.acquire(key);
            if (null != context) {
                try {
                    return immoter.contextualise(invocation, key, context);
                } finally {
                    map.release(context);
                }
            }

            // session was not promoted whilst we were waiting for motionLock. Continue down Contextualiser stack.
            return next.contextualise(invocation, key, immoter, exclusiveOnly);
        } finally {
            invocationLock.unlock();
        }
    }

}
