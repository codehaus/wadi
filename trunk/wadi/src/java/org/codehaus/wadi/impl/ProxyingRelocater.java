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
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Location;
import org.codehaus.wadi.ProxyingException;
import org.codehaus.wadi.RecoverableException;
import org.codehaus.wadi.RelocaterConfig;
import org.codehaus.wadi.RequestRelocater;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Relocate the request to its state, by proxying it to another node
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ProxyingRelocater extends AbstractRelocater implements RequestRelocater {
	protected final Log _log = LogFactory.getLog(getClass());
    protected final long _timeout;
	protected final long _proxyHandOverPeriod;
    protected final Map _rvMap=new HashMap();

	public ProxyingRelocater(long timeout, long proxyHandOverPeriod) {
        _timeout=timeout;
		_proxyHandOverPeriod=proxyHandOverPeriod;
	}

    public void init(RelocaterConfig config) {
        super.init(config);
        MessageDispatcher dispatcher=_config.getDispatcher();
        dispatcher.register(this, "onMessage", LocationRequest.class); // dispatch LocationRequest messages onto our onMessage() method
        dispatcher.register(LocationResponse.class, _rvMap, _timeout); // dispatch LocationResponse classes via synchronous rendez-vous
    }

	protected Location locate(String name, Map locationMap) {
		if (_log.isTraceEnabled()) _log.trace("sending location request: "+name);
		MessageDispatcher.Settings settingsInOut=new MessageDispatcher.Settings();
		settingsInOut.from=_config.getLocation().getDestination();
		settingsInOut.to=_config.getDispatcher().getCluster().getDestination();
		settingsInOut.correlationId=name; // TODO - better correlation id
		LocationRequest request=new LocationRequest(name, _proxyHandOverPeriod);
		LocationResponse response=(LocationResponse)_config.getDispatcher().exchangeMessages(name, _rvMap, request, settingsInOut, _timeout);

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
		if (_log.isTraceEnabled()) _log.trace("updated cache for: "+ids);

		return location;
	}

	public boolean relocate(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String name, Immoter immoter, Sync motionLock, Map locationMap) throws IOException, ServletException {
		Location location;
		boolean refreshed=false;

		if (null==(location=(Location)locationMap.get(name))) {
			location=locate(name, locationMap);
			refreshed=true;
		}

		if (location==null)
			return false;

		boolean recoverable=true;
		try {
			location.proxy(hreq, hres);
			motionLock.release();
			return true;
		} catch (RecoverableException e1) {
			if (!refreshed) {
				if (null==(location=locate(name, locationMap)))
					return false;
				try {
					location.proxy(hreq, hres);
					motionLock.release();
					return true;
				} catch (RecoverableException e2) {
					recoverable=true;
				} catch (ProxyingException e2) {
					if (_log.isErrorEnabled()) _log.error("irrecoverable proxying problem: "+name, e2);
					recoverable=false;
				}
			} else {
				recoverable=true;
			}

		} catch (ProxyingException e) {
			if (_log.isErrorEnabled()) _log.error("irrecoverable proxying problem: "+name, e);
			recoverable=false;
		}

		if (recoverable) {
			// we did find the session's location, but all attempts to proxy to it failed,,,
			motionLock.release();
			if (_log.isErrorEnabled()) _log.error("all attempts at proxying to session location failed - processing request without session: "+name+ " - "+location);
			// we'll have to contextualise hreq here - stateless context ? TODO
			chain.doFilter(hreq, hres);
			return true; // looks wrong - but actually indicates that req should proceed no further down stack...
		} else {
			// TODO - is this correct ?
			motionLock.release();
			return true;
		}
	}

	public void onMessage(ObjectMessage message, LocationRequest request) {
		String id=request.getId();
		if (_log.isTraceEnabled()) _log.trace("receiving location request: "+id);
        Contextualiser top=_config.getContextualiser();
		if (top==null) {
			_log.warn("no Contextualiser set - cannot respond to LocationRequests");
		} else {
			try {
				Destination replyTo=message.getJMSReplyTo();
				String correlationId=message.getJMSCorrelationID();
				long handShakePeriod=request.getHandOverPeriod();
				// TODO - the peekTimeout should be specified by the remote node...
				FilterChain fc=new LocationResponseFilterChain(replyTo, correlationId, _config.getLocation(), id, handShakePeriod);
				top.contextualise(null,null,fc,id, null, null, true);
			} catch (Exception e) {
				if (_log.isWarnEnabled()) _log.warn("problem handling location request: "+id, e);
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
		protected final String _name;
		protected final long _handOverPeriod;

		LocationResponseFilterChain(Destination replyTo, String correlationId, Location location, String name, long handOverPeriod) {
			_replyTo=replyTo;
			_correlationId=correlationId;
			_location=location;
			_name=name;
			_handOverPeriod=handOverPeriod;
		}

		public void
		doFilter(ServletRequest request, ServletResponse response) {
			if (_log.isTraceEnabled()) _log.trace("sending location response: "+_name);
			LocationResponse lr=new LocationResponse(_config.getLocation(), Collections.singleton(_name));
			try {
				ObjectMessage m=_config.getDispatcher().getCluster().createObjectMessage();
				m.setJMSReplyTo(_replyTo);
				m.setJMSCorrelationID(_correlationId);
				m.setObject(lr);
				_config.getDispatcher().getCluster().send(_replyTo, m);

				// Now wait for a while so that the session is locked into this container, giving the other node a chance to proxy to this location and still find it here...
				// instead of just waiting a set period, we could use a Rendezvous object with a timeout - more complexity - consider...
				try {
					if (_log.isTraceEnabled()) _log.trace("waiting for proxy ("+_handOverPeriod+" millis)...: "+_name);
					Thread.sleep(_handOverPeriod);
					if (_log.isTraceEnabled()) _log.trace("...waiting over: "+_name);
				} catch (InterruptedException ignore) {
					// ignore
					// TODO - should we loop here until timeout is up ?
				}

			} catch (JMSException e) {
				if (_log.isErrorEnabled()) _log.error("problem sending location response: "+_name, e);
			}
		}
	}
}
