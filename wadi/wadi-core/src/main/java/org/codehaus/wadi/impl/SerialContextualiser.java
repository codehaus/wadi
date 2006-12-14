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

import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.core.ConcurrentMotableMap;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

/**
 * Ensure that any Contextualisations that pass through are serialised according to the strategy imposed by our Collapser.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SerialContextualiser extends AbstractDelegatingContextualiser {
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
        Sync invocationLock = collapser.getLock(key);
        try {
            // the promotion begins here. allocate a lock and continue...
            try {
                Utils.acquireUninterrupted("Invocation(SerialContextualiser)", key, invocationLock);
            } catch (TimeoutException e) {
                _log.error("could not acquire session within timeframe: " + key);
                return false;
            }
            
            // whilst we were waiting for the motionLock, the session in
            // question may have been moved back into memory somehow.
            // before we proceed, confirm that this has not happened.
            Motable context = map.acquire(key);
            if (null != context) {
                if (_log.isTraceEnabled()) {
                    _log.trace("session has reappeared in memory whilst we were waiting to immote it...: " + key
                            + " [" + Thread.currentThread().getName() + "]");
                }
                // overlap two locking systems until we have secured the
                // session in memory, then run the request
                // and release the lock.
                try {
                    return immoter.contextualise(invocation, key, context);
                } finally {
                    map.release(context);
                }
            }

            // session was not promoted whilst we were waiting for motionLock.
            // Continue down Contextualiser stack
            // it may be below us...
            // lock is to be released as soon as context is available to
            // subsequent contextualisations...
            return next.contextualise(invocation, key, immoter, exclusiveOnly);
        } finally {
            Utils.release("Invocation", key, invocationLock);
        }
    }

}
