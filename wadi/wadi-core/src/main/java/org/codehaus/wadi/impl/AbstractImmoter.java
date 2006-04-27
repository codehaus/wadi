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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Abstract base for Immoters
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public abstract class AbstractImmoter implements Immoter {
	protected static final Log _log = LogFactory.getLog(AbstractImmoter.class);
	
	public boolean prepare(String name, Motable emotable, Motable immotable) {
		return true;
	}
	
	public void commit(String name, Motable immotable) {
		// do nothing
	}
	
	public void rollback(String name, Motable immotable) {
		try {
			immotable.destroy();
		} catch (Exception e) {
			if (_log.isErrorEnabled()) _log.error("problem rolling back immotion: "+name, e);
		}
	}
	
	// keep the throws clause - we are defining a method signature for our subtypes
	public boolean contextualise(Invocation invocation, String id, Motable immotable, Sync motionLock) throws InvocationException {
		// most Contextualisers cannot contextualise locally...
		return false;
	}
}
