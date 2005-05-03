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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.Collapser;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutSync;

/**
 * A lock Collapser that collapses according to hash code
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class HashingCollapser implements Collapser {
	protected final Log    _log = LogFactory.getLog(getClass());
	protected final int    _numSyncs;
	protected final Sync[] _syncs;
	protected final long   _timeout;

	/**
	 *
	 */
	public HashingCollapser(int numSyncs, long timeout) {
		super();
		_numSyncs=numSyncs;
		_timeout=timeout;
		_syncs=new Sync[_numSyncs];
		for (int i=0; i<_numSyncs; i++)
			_syncs[i]=new TimeoutSync(new Mutex(), _timeout);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Collapser#getLock(java.lang.String)
	 */
	public Sync getLock(String id) {
		int index=Math.abs(id.hashCode()%_numSyncs); // Jetty seems to generate negative session id hashcodes...
		if (_log.isTraceEnabled()) _log.trace("collapsed "+id+" to index: "+index);
		return _syncs[index];
	}
}
