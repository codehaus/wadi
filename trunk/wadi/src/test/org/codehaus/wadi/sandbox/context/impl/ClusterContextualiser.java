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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.activecluster.Cluster;
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Location;
import org.codehaus.wadi.sandbox.context.MigrationStrategy;
import org.codehaus.wadi.sandbox.context.Motable;
import org.codehaus.wadi.sandbox.context.Promoter;
import org.codehaus.wadi.sandbox.context.ProxyStrategy;

import EDU.oswego.cs.dl.util.concurrent.Rendezvous;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

// this needs to be split into several parts...

// (1) a request relocation strategy (proxy)
// (2) a state relocation strategy (migrate) - used if (1) fails
// a location cache - used by both the above when finding the locarion of state...

/**
 * A cache of Locations. If the Location of a Context is not known, the Cluster
 * may be queried for it. If it is forthcoming, we can proxy to it. After a
 * given number of successful proxies, the Context will be migrated to this
 * Contextualiser which should promote it so that future requests for it can be
 * run straight off the top of the stack.
 *
 * Node N1 sends LocationRequest to Cluster
 * Node N2 contextualises this request with a FilterChain that will send a LocationResponse and wait a specified handover period.
 * Node N1 receives the response, updates its cache and then proxies through the Location to the required resource.
 *
 * The promotion mutex is held correctly during the initial Location lookup, so that searches for the same Context are correctly collapsed.
 *
 * ProxyStrategy should be applied before MigrationStrategy - if it succeeds, we don't migrate...
 *
 * This class is getting out of hand !
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ClusterContextualiser extends AbstractMappedContextualiser {

	protected final Cluster           _cluster;
	protected final MessageConsumer   _consumer;
	protected final MessageListener   _listener;
	protected final Map               _searches=new HashMap(); // do we need more concurrency ?
	protected final long              _timeout;
	protected final Location          _location;
	protected final long              _proxyHandOverPeriod;
	protected final ProxyStrategy     _proxyer;
	protected final MigrationStrategy _migrater;

	protected Contextualiser _top;
	public void setContextualiser(Contextualiser top){_top=top;}
	public Contextualiser getContextualiser(){return _top;}

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
				ObjectMessage m=_cluster.createObjectMessage();
				m.setJMSReplyTo(_replyTo);
				m.setJMSCorrelationID(_correlationId);
				m.setObject(lr);
				_cluster.send(_replyTo, m);

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

	class LocationListener implements MessageListener {
		// TODO - should we use a semaphore here to bound the number of threads that we start ?
		public void onMessage(Message message) {
			new LocationListenerThread(message).start();
		}
	}

	class LocationListenerThread extends Thread {
		protected final Message _message;

		public LocationListenerThread(Message message) {
			_message=message;
		}

		public void run() {
			ObjectMessage om=null;
			Object tmp=null;
			LocationResponse response=null;
			LocationRequest request=null;

			try {
				if (_message instanceof ObjectMessage && (om=(ObjectMessage)_message)!=null && (tmp=om.getObject())!=null) {
					if (tmp instanceof LocationRequest && (request=(LocationRequest)tmp)!=null)
						onLocationRequestMessage(om, request);
					else if (tmp instanceof LocationResponse && (response=(LocationResponse)tmp)!=null)
						onLocationResponseMessage(om, response);
				}
				// should we try evicting entried before inserting them - or is this just a waste of time...
				// TODO - maybe call evict ?
			} catch (JMSException e) {
				_log.error("problem processing location message", e);
			}
		}

		public void onLocationRequestMessage(ObjectMessage message, LocationRequest request) throws JMSException {
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

		public void onLocationResponseMessage(ObjectMessage message, LocationResponse response) throws JMSException {
			// unpack message content into local cache...
			_log.info("receiving location response: "+response.getIds());

			// notify waiting threads...
			String correlationId=message.getJMSCorrelationID();
			synchronized (_searches) {
				Rendezvous rv=(Rendezvous)_searches.get(correlationId);
				if (rv!=null) {
					do {
						try {
							rv.attemptRendezvous(response, _timeout);
						} catch (TimeoutException toe) {
							_log.info("rendez-vous timed out: "+correlationId, toe);
						} catch (InterruptedException ignore) {
							_log.info("rendez-vous interruption ignored: "+correlationId);
						}
					} while (Thread.interrupted()); // TODO - should really subtract from timeout each time...
				}
			}
		}
	}

	/**
	 * @param next
	 * @param collapser
	 * @param map
	 * @param evicter
	 */
	public ClusterContextualiser(Contextualiser next, Collapser collapser, Map map, Evicter evicter, Cluster cluster, long timeout, long proxyHandOverPeriod, Location location, ProxyStrategy proxyer, MigrationStrategy migrater) throws JMSException {
		super(next, collapser, map, evicter);
		_cluster=cluster;
		boolean excludeSelf=true;
	    _consumer=_cluster.createConsumer(_cluster.getDestination(), null, excludeSelf);
		_listener=new LocationListener();
	    _consumer.setMessageListener(_listener);// should be called in start() - we need a stop() too - to remove listeners...
	    _timeout=timeout;
	    _proxyHandOverPeriod=proxyHandOverPeriod;
	    _location=location;
	    _proxyer=proxyer;
	    _migrater=migrater;
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.impl.AbstractChainedContextualiser#getPromoter(org.codehaus.wadi.sandbox.context.Promoter)
	 */
	public Promoter getPromoter(Promoter promoter) {
		return promoter; // TODO - would we ever want to allow promotion of a context into our cache or out to the cluster ?
}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.impl.AbstractChainedContextualiser#contextualiseLocally(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Promoter, EDU.oswego.cs.dl.util.concurrent.Sync)
	 */
	public boolean contextualiseLocally(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Promoter promoter, Sync promotionLock) throws IOException, ServletException {
		Location location=(Location)_map.get(id);
		if (location!=null) {
			// let's try the cached location...
			 if (contextualiseLocally(hreq, hres, chain, id, promoter, promotionLock, location)) {
			 	return true;
			 } else {
				// cached location may have been stale i.e. not responding to http requests
				// this should be a very infrequent occurrence...
				location=null;
			}
		}
		
		if (location==null) {
			// refresh the cache...
			location=locate(id);
		}

		// if we could not locate the context, we are at the end of the road...
		if (location==null) {
			_log.info("could not locate: "+id);
			return false;
		}
		
		if ((contextualiseLocally(hreq, hres, chain, id, promoter, promotionLock, location))) {
			return true;
		} else {
			// proxy failed
			// the promotionLock has NOT yet been released...
			promotionLock.release();
			// we'll have to contextualise hreq here - stateless context ? TODO
			_log.error("could not proxy to: "+location+" for id:"+id+" - contextualising locally");
			chain.doFilter(hreq, hres);
			
			return true; // looks wrong - but actually indicates that req should proceed no further down stack...
		}
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#evict()
	 */
	public void evict() {
		// how long do we wish to maintain cached Locations ?
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#demote(java.lang.String, org.codehaus.wadi.sandbox.context.Motable)
	 */
	public void demote(String key, Motable val) {
		// push stuff out to cluster - i.e. emmigrate - tricky...
		// for the moment - just push to the tier below us...
		_next.demote(key, val);
	}

	public boolean isLocal(){return false;}
	
	// ClusterContextualiser...
	
	protected Location locate(String id) {
		Location location=null;
		
		String correlationId=_cluster.getLocalNode().getDestination().toString()+"-"+id;
		Rendezvous rv=new Rendezvous(2);

		// set up a rendez-vous...
		synchronized (_searches) {
			_searches.put(correlationId, rv);
		}

		try {
			// broadcast location query to whole cluster
			LocationRequest request=new LocationRequest(id, _proxyHandOverPeriod);
			ObjectMessage message=_cluster.createObjectMessage();
			message.setJMSReplyTo(_cluster.getDestination());
			message.setJMSCorrelationID(correlationId);
			message.setObject(request);
			_log.info("sending location request: "+id);
			_cluster.send(_cluster.getDestination(), message);

			// rendez-vous with response/timeout...
			do {
				try {
					LocationResponse response=(LocationResponse)rv.attemptRendezvous(null, _timeout);
					location=response.getLocation();
					Set ids=response.getIds();
					// update cache
					// TODO - do we need to considering NOT putting any location that is the same ours into the map
					// otherwise we may end up in a tight loop proxying to ourself... - could this happen ?

					for (Iterator i=ids.iterator(); i.hasNext();) {
						String tmp=(String)i.next();
						_map.put(tmp, location);
					}
					_log.info("updated cache for: "+ids);
				} catch (TimeoutException toe) {
					_log.info("no response to location query within timeout: "+id); // session does not exist
				} catch (InterruptedException ignore) {
					_log.info("waiting for location response - interruption ignored: "+id);
				}
			} while (Thread.interrupted());
		} catch (JMSException e) {
			_log.warn("problem sending session location query: "+id, e);
		}

		// tidy up rendez-vous
		synchronized (_searches) {
			_searches.remove(correlationId);
		}

		return location;
	}
	
	protected boolean contextualiseLocally(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Promoter promoter, Sync promotionLock, Location location) throws IOException {
		boolean success=false;
		if (!(success=_proxyer.proxy(hreq, hres, id, promotionLock, location)))
			success=_migrater.migrateAndPromote(hreq, hres, chain, id, promoter, promotionLock, location);
		
		return success;
	}
}
