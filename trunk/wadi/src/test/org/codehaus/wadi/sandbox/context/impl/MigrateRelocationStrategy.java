/**
*
* Copyright 2003-2004 The Apache Software Foundation
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
package org.codehaus.wadi.sandbox.context.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jms.ObjectMessage;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Promoter;
import org.codehaus.wadi.sandbox.context.RelocationStrategy;
import org.codehaus.wadi.sandbox.context.SessionRelocationStrategy;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class MigrateRelocationStrategy implements SessionRelocationStrategy {
	protected final Log _log=LogFactory.getLog(getClass());
	protected final MessageDispatcher _dispatcher;
	protected final Map _rvMap=new HashMap();
	protected final long _timeout;
	
	public MigrateRelocationStrategy(MessageDispatcher dispatcher, long timeout) {
		_dispatcher=dispatcher;		
		_timeout=timeout;
		_dispatcher.register(this, "onMessage");
		_dispatcher.register(MigrationResponse.class, _rvMap, _timeout);
	}

	public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Promoter promoter, Sync promotionLock, Map locationMap) throws IOException, ServletException {
//		_log.info("sending location request: "+id);
//		MigrationRequest request=new MigrationRequest(id, 3000);
//		Destination dest=location.getDestination();
//		MigrationResponse response=(MigrationResponse)_dispatcher.exchangeMessages(id, _rvMap, request, dest, _timeout);
//		// get session out of response and promote...
		
		return false;
	}
		
	public void onMessage(ObjectMessage om, MigrationRequest request) {
		// send a migration response containing session...
		String id=request.getId();
		_log.info("RECEIVED MIGRATION REQUEST FOR; "+id);
	}

	public void setTop(Contextualiser top){}
}
