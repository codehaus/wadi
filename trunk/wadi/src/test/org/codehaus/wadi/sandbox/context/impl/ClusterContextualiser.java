/*
 * Created on Feb 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.codehaus.wadi.sandbox.context.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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


import EDU.oswego.cs.dl.util.concurrent.Sync;

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

	protected final Cluster _cluster;
	protected final MessageConsumer _consumer;
	protected final MessageListener _listener;
	
	class LocationListener implements MessageListener {
		public void onMessage(Message message) {
			ObjectMessage om=null;
			Object tmp=null;
			Locations locations=null;
			
			try {
				// filter/validate message
				if (message instanceof ObjectMessage &&
					(om=(ObjectMessage)message)!=null &&
					(tmp=om.getObject())!=null &&
					tmp instanceof Locations &&
					(locations=(Locations)tmp)!=null) {
					// unpack message content into local cache...
					Location location=locations.getLocation();
					Set ids=locations.getIds();
					for (Iterator i=ids.iterator(); i.hasNext();) {
						String id=(String)i.next();
						_map.put(id, location);
					}
					
					// how about :
					// request for Location uses unique correlation id and stores rendezvous in _table
					// after processing a Location, we check _table for correlation id and rendezvous if present...
					
					_log.info("updated cache for: "+ids);
				}			
				// should we try evicting entried before inserting them - or is this just a waste of time...
				// TODO - maybe call evict ?
			} catch (JMSException e) {
				_log.info("bad message received: "+message);
			}
		}
	}
	
	/**
	 * @param next
	 * @param collapser
	 * @param map
	 * @param evicter
	 */
	public ClusterContextualiser(Contextualiser next, Collapser collapser, Map map, Evicter evicter, Cluster cluster) throws JMSException {
		super(next, collapser, map, evicter);
		_cluster=cluster;
		boolean excludeSelf=true;
	    _consumer=_cluster.createConsumer(_cluster.getDestination(), null, excludeSelf);
		_listener=new LocationListener();
	    _consumer.setMessageListener(_listener);// should be called in start() - we need a stop() too - to remove listeners...
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
		
		Location l=null;
		
		if ((l=(Location)_map.get(id))==null) {
			// TODO :
			// send out LocationMessage to Cluster
			// wait for a response (Locations - broadcast to everyone - includes unique correlation id) or timeout
			
			// question - if we receive a request for location, how do we answer it - without a req/res pair to contextualise...
			
			if ((l=(Location)_map.get(id))==null) {
				return false;
			} 
		}
		
		assert l!=null;
		
		_log.info("location found (cluster): "+id+" - "+l);
		
		// initially we will release the promotion lock here, so that proxying is done concurrently
		// later we need to decide how to count successful proxies and retrieve a remote session when count is achieved...
		if (promotionMutex!=null) {
			promotionMutex.release();
		}
		
		// either:
		// proxy the request/response to the sessions current location...
		l.proxy(req,res);
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
