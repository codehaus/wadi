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
	  boolean release=false;
	  
	  try {
	    if (invocationLock!=null) {
	      release=true;
	    } else {
	      // the promotion begins here...
	      // allocate a lock and continue...
	      invocationLock=_collapser.getLock(id);
	      
	      Utils.acquireUninterrupted("Invocation(SerialContextualiser)", id, invocationLock);
	      release=true;
	      
	      // whilst we were waiting for the motionLock, the session in question may have been moved back into memory somehow.
	      // before we proceed, confirm that this has not happened.
	      Context context=(Context)_map.get(id);
	      if (null!=context) {
	        // oops - it HAS happened...
	        if (_log.isTraceEnabled()) _log.trace("session has reappeared in memory whilst we were waiting to immote it...: "+id+ " ["+Thread.currentThread().getName()+"]");
	        // overlap two locking systems until we have secured the session in memory, then run the request
	        // and release the lock.
	        
	        if (immoter.contextualise(invocationContext, id, context, invocationLock)) {
	          release=false;
	          return true;
	        }
	      }
	    }
	    
	    // session was not promoted whilst we were waiting for motionLock. Continue down Contextualiser stack
	    // it may be below us...
	    // lock is to be released as soon as context is available to subsequent contextualisations...
	    boolean found=_next.contextualise(invocationContext, id, immoter, invocationLock, exclusiveOnly);
	    release=!found;
	    return found;
	  } catch (TimeoutException e) {
	    _log.error("could not acquire session within timeframe: "+id);
	    return false;
	  } finally {
	    if (release) {
	      Utils.release("Invocation", id, invocationLock);
	    }
	  }
	}
	
}
