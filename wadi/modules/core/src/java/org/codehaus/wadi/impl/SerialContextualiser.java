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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.Context;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.InvocationContext;
import org.codehaus.wadi.InvocationException;

import EDU.oswego.cs.dl.util.concurrent.NullSync;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

/**
 * Ensure that any Contextualisations that pass through are serialised according to the strategy imposed by our Collapser.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SerialContextualiser extends AbstractDelegatingContextualiser {

	protected final Collapser _collapser;
	protected final Sync _dummyLock=new NullSync();
	protected final Map _map;
	protected final Log _lockLog=LogFactory.getLog("org.codehaus.wadi.LOCKS");

	public SerialContextualiser(Contextualiser next, Collapser collapser, Map map) {
		super(next);
		_collapser=collapser;
		_map=map;
	}

	public boolean contextualise(InvocationContext invocationContext, String id, Immoter immoter, Sync invocationLock, boolean exclusiveOnly) throws InvocationException {
		if (invocationLock!=null) {
			// someone is already doing a promotion from further up the
			// stack - do nothing other than delegate...
			return _next.contextualise(invocationContext, id, immoter, invocationLock, exclusiveOnly);
		} else {
			// the promotion begins here...
			// allocate a lock and continue...
			invocationLock=_collapser.getLock(id);
			boolean invocationLockAcquired=false;
			try {
				Utils.acquireUninterrupted("Invocation", id, invocationLock);
				invocationLockAcquired=true;
			} catch (TimeoutException e) {
				_log.error("unexpected timeout - proceding without lock", e);
			}

			try {
				// whilst we were waiting for the motionLock, the session in question may have been moved back into memory somehow.
				// before we proceed, confirm that this has not happened.
				Context context=(Context)_map.get(id);
				boolean found=false;
				if (null!=context) {
					// oops - it HAS happened...
					if (_log.isWarnEnabled()) _log.warn("session has reappeared in memory whilst we were waiting to immote it...: "+id+ " ["+Thread.currentThread().getName()+"]"); // TODO - downgrade..
					// overlap two locking systems until we have secured the session in memory, then run the request
					// and release the lock.
					// TODO - we really need to take a read lock before we release the motionLock...
					found=immoter.contextualise(invocationContext, id, context, invocationLock);
					// although we did find the context it may have left this contextualiser before we finally acquire its lock.
					// be prepared to continue dowm the stack looking for it.
				}

				if (!found) {
					// session was not promoted whilst we were waiting for motionLock. Continue down Contextualiser stack
					// it may be below us...
					// lock is to be released as soon as context is available to subsequent contextualisations...
					found=_next.contextualise(invocationContext, id, immoter, invocationLock, exclusiveOnly);
				}
				invocationLockAcquired=!found;
				return found;
			} finally {
				if (invocationLockAcquired) {
					Utils.release("Invocation", id, invocationLock);
				}
			}
		}
	}
}
