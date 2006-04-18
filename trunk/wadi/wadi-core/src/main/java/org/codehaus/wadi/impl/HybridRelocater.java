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
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.InvocationContext;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.InvocationProxy;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.RelocaterConfig;
import org.codehaus.wadi.group.Dispatcher;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

/**
 * Combine various RelocationStrategies to produce a cleverer one
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class HybridRelocater extends AbstractRelocater {

	protected final Log _log=LogFactory.getLog(getClass());
	protected final long _requestHandOverTimeout=2000;// TODO - parameterise
	protected final long _resTimeout;
	protected final long _ackTimeout;
	protected final boolean _sessionOrRequestPreferred; // true if relocation of session is preferred to relocation of request
	protected final Log _lockLog=LogFactory.getLog("org.codehaus.wadi.LOCKS");

	public HybridRelocater(long resTimeout, long ackTimeout, boolean sessionOrRequestPreferred) {
		_resTimeout=resTimeout;
		_ackTimeout=ackTimeout;
		_sessionOrRequestPreferred=sessionOrRequestPreferred;
	}

	protected SynchronizedBoolean _shuttingDown;
	protected Dispatcher _dispatcher;
	protected String _nodeName;
	protected Contextualiser _contextualiser;
	protected InvocationProxy _proxy;

	public void init(RelocaterConfig config) {
		super.init(config);
		_shuttingDown=_config.getShuttingDown();
		_dispatcher=_config.getDispatcher();
		_nodeName=_config.getNodeName();
		_contextualiser=_config.getContextualiser();
		_proxy=_config.getInvocationProxy();
	}

	public boolean relocate(InvocationContext invocationContext, String name, Immoter immoter, Sync motionLock) throws InvocationException {
	  String sessionName=name;
	  String nodeName=_config.getNodeName();
	  boolean shuttingDown=_shuttingDown.get();
	  int concurrentRequestThreads=1;
	  
	  Motable immotable=null;
	  try {
	    immotable=_config.getDIndex().relocate(sessionName, nodeName, concurrentRequestThreads, shuttingDown, _resTimeout);
	  } catch (Exception e) {
	    _log.error("unexpected error", e);
	  }
	  
	  if (null==immotable) {
	    return false;
	  } else {
	    boolean answer=immoter.contextualise(invocationContext, name, immotable, motionLock);
	    return answer;
	  }
	}

	/* We send a RelocationRequest out to fetch a Session. We receive a RelocationResponse containing the Session. We pass a RelocationEmoter
	 * down the Contextualiser stack. It passes the incoming Session out to the relevant Contextualiser and sends a RelocationAcknowledgment
	 * back to the src of the RelocationResponse. (could be done in a Motable like Immoter?)
	 */

	boolean getSessionOrRequestPreferred() {
		return _sessionOrRequestPreferred;
		// check out LB's capabilities during init()....
	}

}
