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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.activecluster.Cluster;
import org.codehaus.wadi.sandbox.context.Location;
import org.codehaus.wadi.sandbox.context.Promoter;
import org.codehaus.wadi.sandbox.context.ProxyingException;
import org.codehaus.wadi.sandbox.context.RecoverableException;
import org.codehaus.wadi.sandbox.context.RelocationStrategy;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * TODO - JavaDoc this type
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */

public class RequestRelocationStrategy implements RelocationStrategy {
	protected final Log _log = LogFactory.getLog(getClass());
	protected final MessageDispatcher _dispatcher;
	protected final long _proxyHandOverPeriod;
	protected final long _timeout;
	protected final Map _rvMap=new HashMap();
	protected final Cluster _cluster;
	
	public RequestRelocationStrategy(Cluster cluster, MessageDispatcher dispatcher, long proxyHandOverPeriod, long timeout) {
		_dispatcher=dispatcher;
		_proxyHandOverPeriod=proxyHandOverPeriod;
		_timeout=timeout;
		_cluster=cluster;
		
		_dispatcher.register(LocationResponse.class, _rvMap, _timeout);
	}
	
	protected Location locate(String id, Map locationMap) {
		_log.info("sending location request: "+id);
		LocationRequest request=new LocationRequest(id, _proxyHandOverPeriod);
		LocationResponse response=(LocationResponse)_dispatcher.exchangeMessages(id, _rvMap, request, _cluster.getDestination(), _timeout);
		
		if (response==null)
			return null;
		
		Location location=response.getLocation();
		Set ids=response.getIds();
		// update cache
		// TODO - do we need to considering NOT putting any location that is the same ours into the map
		// otherwise we may end up in a tight loop proxying to ourself... - could this happen ?
		
		Iterator i=ids.iterator();
		synchronized (locationMap) {
			while (i.hasNext()) {
				locationMap.put(i.next(), location);
			}
		}
		_log.info("updated cache for: "+ids);
		
		return location;
	}
	
	public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Promoter promoter, Sync promotionLock, Map locationMap) throws IOException, ServletException {
		Location location;
		boolean refreshed=false;
		
		if (null==(location=(Location)locationMap.get(id))) {
			location=locate(id, locationMap);
			refreshed=true;
		}
		
		if (location==null)
			return false;
		
		boolean recoverable=true;
		try {
			location.proxy(hreq, hres);
			promotionLock.release();
			return true;
		} catch (RecoverableException e1) {
			if (!refreshed) {
				if (null==(location=locate(id, locationMap)))
					return false;			
				try {
					location.proxy(hreq, hres);
					promotionLock.release();
					return true;
				} catch (RecoverableException e2) {
					recoverable=true;
				} catch (ProxyingException e2) {
					_log.error("irrecoverable proxying problem: "+id, e2);
					recoverable=false;
				}
			} else {
				recoverable=true;
			}
			
		} catch (ProxyingException e) {
			_log.error("irrecoverable proxying problem: "+id, e);
			recoverable=false;
		}
		
		if (recoverable) {
			// we did find the session's location, but all attempts to proxy to it failed,,,
			promotionLock.release();
			_log.error("all attempts at proxying to session location failed - processing request without session: "+id+ " - "+location);
			// we'll have to contextualise hreq here - stateless context ? TODO
			chain.doFilter(hreq, hres);			
			return true; // looks wrong - but actually indicates that req should proceed no further down stack...
		} else {
			// TODO - is this correct ?
			promotionLock.release();
			return true;
		}
	}
}
