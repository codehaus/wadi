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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Location;
import org.codehaus.wadi.sandbox.ProxyingException;
import org.codehaus.wadi.sandbox.RecoverableException;
import org.codehaus.wadi.sandbox.RequestRelocationStrategy;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Relocate the request to its state, by proxying it to another node
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ProxyRelocationStrategy implements RequestRelocationStrategy {
	protected final Log _log = LogFactory.getLog(getClass());
	protected final MessageDispatcher _dispatcher;
	protected final long _proxyHandOverPeriod;
	protected final long _timeout;
	protected final Map _rvMap=new HashMap();
	protected final Location _location;
	protected Contextualiser _top;

	public void setTop(Contextualiser top){_top=top;}
	public Contextualiser getTop(){return _top;}

	public ProxyRelocationStrategy(MessageDispatcher dispatcher, Location location, long timeout, long proxyHandOverPeriod) {
		_dispatcher=dispatcher;
		_proxyHandOverPeriod=proxyHandOverPeriod;
		_timeout=timeout;
		_location=location;

		_dispatcher.register(this, "onMessage"); // dispatch LocationRequest messages onto our onMessage() method
		_dispatcher.register(LocationResponse.class, _rvMap, _timeout); // dispatch LocationResponse classes via synchronous rendez-vous
	}

	protected Location locate(String id, Map locationMap) {
		_log.info("sending location request: "+id);
		MessageDispatcher.Settings settingsInOut=new MessageDispatcher.Settings();
		settingsInOut.from=_location.getDestination();
		settingsInOut.to=_dispatcher.getCluster().getDestination();
		settingsInOut.correlationId=id; // TODO - better correlation id
		LocationRequest request=new LocationRequest(id, _proxyHandOverPeriod);
		LocationResponse response=(LocationResponse)_dispatcher.exchangeMessages(id, _rvMap, request, settingsInOut, _timeout);

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

	public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock, Map locationMap) throws IOException, ServletException {
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

	public void onMessage(ObjectMessage message, LocationRequest request) throws JMSException {
		String id=request.getId();
		_log.info("receiving location request: "+id);
		if (_top==null) {
			_log.warn("no Contextualiser set - cannot respond to LocationRequests");
		} else {
			try {
				Destination replyTo=message.getJMSReplyTo();
				String correlationId=message.getJMSCorrelationID();
				long handShakePeriod=request.getHandOverPeriod();
				// TODO - the peekTimeout should be specified by the remote node...
				FilterChain fc=new LocationResponseFilterChain(replyTo, correlationId, _location, id, handShakePeriod);
				_top.contextualise(null,null,fc,id, null, null, true);
			} catch (Exception e) {
				_log.warn("problem handling location request: "+id);
			}
			// TODO - if we see a LocationRequest for a session that we know is Dead - we should respond immediately.
		}
	}

	class LocationResponseFilterChain
	implements FilterChain
	{
		protected final Destination _replyTo;
		protected final String _correlationId;
		protected final Location _location;
		protected final String _id;
		protected final long _handOverPeriod;

		LocationResponseFilterChain(Destination replyTo, String correlationId, Location location, String id, long handOverPeriod) {
			_replyTo=replyTo;
			_correlationId=correlationId;
			_location=location;
			_id=id;
			_handOverPeriod=handOverPeriod;
		}

		public void
		doFilter(ServletRequest request, ServletResponse response)
		throws IOException, ServletException
		{
			_log.info("sending location response: "+_id);
			LocationResponse lr=new LocationResponse(_location, Collections.singleton(_id));
			try {
				ObjectMessage m=_dispatcher.getCluster().createObjectMessage();
				m.setJMSReplyTo(_replyTo);
				m.setJMSCorrelationID(_correlationId);
				m.setObject(lr);
				_dispatcher.getCluster().send(_replyTo, m);

				// Now wait for a while so that the session is locked into this container, giving the other node a chance to proxy to this location and still find it here...
				// instead of just waiting a set period, we could use a Rendezvous object with a timeout - more complexity - consider...
				try {
					_log.info("waiting for proxy ("+_handOverPeriod+" millis)...: "+_id);
					Thread.sleep(_handOverPeriod);
					_log.info("...waiting over: "+_id);
				} catch (InterruptedException ignore) {
					// ignore
					// TODO - should we loop here until timeout is up ?
				}

			} catch (JMSException e) {
				_log.error("problem sending location response: "+_id, e);
			}
		}
	}
}
