/*
 * Created on Feb 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
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

import org.codehaus.activecluster.Cluster;
import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Location;
import org.codehaus.wadi.sandbox.context.Motable;
import org.codehaus.wadi.sandbox.context.Promoter;

import EDU.oswego.cs.dl.util.concurrent.Rendezvous;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

//NOTES - 

// how do we attach a listener for location requests that can decide whether an id is present in cache and reply with a location response object ?

// we need to resolve the immigration of sessions after a given number of requests have been proxied successfully...

// we need a way of testing this...

/**
 * @author jules
 * 
 * A cache of Locations. If the Location of a Context is not known, the Cluster
 * may be queried for it. If it is forthcoming, we can proxy to it. After a
 * given number of successful proxies, the Context will be migrated to this
 * Contextualiser which should promote it so that future requests for it can be
 * run straight off the top of the stack.
 */
public class ClusterContextualiser extends AbstractMappedContextualiser {

	protected final Cluster         _cluster;
	protected final MessageConsumer _consumer;
	protected final MessageListener _listener;
	protected final Map             _searches=new HashMap(); // do we need more concurrency ?
	protected final long            _timeout;
	protected final Location        _location;
	
	protected Contextualiser _top;
	public void setContextualiser(Contextualiser top){_top=top;}
	public Contextualiser getContextualiser(){return _top;}
	
	class LocationListener implements MessageListener {
		public void onMessage(Message message) {
			ObjectMessage om=null;
			Object tmp=null;
			LocationResponse response=null;
			LocationRequest request=null;
			
			try {
				if (message instanceof ObjectMessage && (om=(ObjectMessage)message)!=null && (tmp=om.getObject())!=null) {
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
				// TODO - somehow we have to query local contextualisers for this session, causing its promotion if it is present and send a message back if so...
				_log.info("GOT TO HERE");
			}
			
			Destination destination=message.getJMSReplyTo();
			String correlationId=message.getJMSCorrelationID();
			boolean present=false;
			if (present) {
				LocationResponse response=new LocationResponse(_location, Collections.singleton(id));
				try {
					ObjectMessage m=_cluster.createObjectMessage();
					m.setJMSReplyTo(destination);
					m.setJMSCorrelationID(correlationId);
					m.setObject(response);
					_cluster.send(destination, m);
				} catch (JMSException e) {
					_log.error("problem sending location response: "+id, e);
				}
			}
		}
		
		public void onLocationResponseMessage(ObjectMessage message, LocationResponse response) throws JMSException {
			// unpack message content into local cache...
			Location location=response.getLocation();
			Set ids=response.getIds();
			_log.info("receiving location response: "+ids);
			
			// notify waiting threads...
			String correlationId=message.getJMSCorrelationID();
			synchronized (_searches) {
				Rendezvous rv=(Rendezvous)_searches.get(correlationId);
				if (rv!=null) {
					do {
						try {
							rv.attemptRendezvous(location, _timeout);
						} catch (TimeoutException toe) {
							_log.info("rendez-vous timed out: "+correlationId, toe);
						} catch (InterruptedException ignore) {
							_log.info("rendez-vous interruption ignored: "+correlationId);
						}
					} while (Thread.interrupted()); // TODO - should really subtract from timeout each time...
				}
			}
			
			// update cache
			for (Iterator i=ids.iterator(); i.hasNext();) {
				String id=(String)i.next();
				_map.put(id, location);
			}
			_log.info("updated cache for: "+ids);
		}
	}
	
	/**
	 * @param next
	 * @param collapser
	 * @param map
	 * @param evicter
	 */
	public ClusterContextualiser(Contextualiser next, Collapser collapser, Map map, Evicter evicter, Cluster cluster, long timeout) throws JMSException {
		super(next, collapser, map, evicter);
		_cluster=cluster;
		boolean excludeSelf=true;
	    _consumer=_cluster.createConsumer(_cluster.getDestination(), null, excludeSelf);
		_listener=new LocationListener();
	    _consumer.setMessageListener(_listener);// should be called in start() - we need a stop() too - to remove listeners...
	    _timeout=timeout;
	    // TODO
	    _location=null; // should be a Location allowing proxying back to this node...
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.impl.AbstractChainedContextualiser#getPromoter(org.codehaus.wadi.sandbox.context.Promoter)
	 */
	public Promoter getPromoter(Promoter promoter) {
		return promoter; // would we ever want to allow promotion of a context into our cache or out to the cluster ?
}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.impl.AbstractChainedContextualiser#contextualiseLocally(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain, java.lang.String, org.codehaus.wadi.sandbox.context.Promoter, EDU.oswego.cs.dl.util.concurrent.Sync)
	 */
	public boolean contextualiseLocally(ServletRequest req, ServletResponse res, FilterChain chain, String id, Promoter promoter, Sync promotionMutex) throws IOException, ServletException {
		
		Location location=null;
		
		if ((location=(Location)_map.get(id))==null) {
			String correlationId=_cluster.getLocalNode().getDestination().toString()+"-"+id;
			Rendezvous rv=new Rendezvous(2);

			// set up a rendez-vous...
			synchronized (_searches) {
				_searches.put(correlationId, rv);
			}
			
			try {
				// broadcast location query to whole cluster
				LocationRequest query=new LocationRequest(id);
				ObjectMessage message=_cluster.createObjectMessage();
				message.setJMSReplyTo(_cluster.getDestination());
				message.setJMSCorrelationID(correlationId);
				message.setObject(query);
				_log.info("sending location request: "+id);
				_cluster.send(_cluster.getDestination(), message);
				
				// rendez-vous with response/timeout...
				do {
					try {
						location=(Location)rv.attemptRendezvous(null, _timeout);
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
			
			if (location==null) {
				return false;
			} 
		}
		
		assert location!=null;
		
		_log.info("location found (cluster): "+id+" - "+location);
		
		// initially we will release the promotion lock here, so that proxying is done concurrently
		// later we need to decide how to count successful proxies and retrieve a remote session when count is achieved...
		if (promotionMutex!=null) {
			promotionMutex.release();
		}
		
		// either:
		// proxy the request/response to the sessions current location...
		location.proxy(req,res);
		// or:
		// retrieve and promote the session
		// send out a LocationMessage to cluster
		// contextualise the request/response
		
		return true;
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
}
