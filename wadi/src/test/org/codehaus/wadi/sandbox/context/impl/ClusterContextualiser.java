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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.wadi.sandbox.context.Collapser;
import org.codehaus.wadi.sandbox.context.Contextualiser;
import org.codehaus.wadi.sandbox.context.Evicter;
import org.codehaus.wadi.sandbox.context.Motable;
import org.codehaus.wadi.sandbox.context.Promoter;
import org.codehaus.wadi.sandbox.context.RelocationStrategy;

import EDU.oswego.cs.dl.util.concurrent.Sync;

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

	protected final MessageDispatcher _dispatcher;
	protected final RelocationStrategy _relocater;

	/**
	 * @param next
	 * @param collapser
	 * @param map
	 * @param evicter
	 */
	public ClusterContextualiser(Contextualiser next, Collapser collapser, Map map, Evicter evicter, MessageDispatcher dispatcher, RelocationStrategy relocater) throws JMSException {
		super(next, collapser, map, evicter);
		_dispatcher=dispatcher;
	    _relocater=relocater;
	    
	    _dispatcher.register(this, "onMessage");
	    _dispatcher.register(EmmigrationAcknowledgement.class, _emmigrationRvMap, 3000);
		}
	
	protected Contextualiser _top;
	public Contextualiser getTop() {return _top;}
	public void setTop(Contextualiser top) {_top=top;}

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
		return _relocater.relocate(hreq, hres, chain, id, promoter, promotionLock, _map);
	}

	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#evict()
	 */
	public void evict() {
		// how long do we wish to maintain cached Locations ?
	}

	protected Destination _emmigrationQueue;

	public void setEmmigrationQueue(Destination emmigrationQueue) throws Exception {
		_emmigrationQueue=emmigrationQueue;
		// send message inviting everyone to start reading from queue
		MessageDispatcher.Settings settings=new MessageDispatcher.Settings();
		settings.to=_dispatcher.getCluster().getDestination();
		_dispatcher.sendMessage(new EmmigrationStartedNotification(_emmigrationQueue), settings);
//		Thread.sleep(1000);
//		_dispatcher.sendMessage(new ShutDownEndedNotification(_emmigrationQueue), settings);
		// dump subsequent demoted Contexts onto queue
		}
	
	public void onMessage(ObjectMessage om, EmmigrationStartedNotification sdsn) throws JMSException {
		Destination emmigrationQueue=sdsn.getDestination();
		_log.info("received EmmigrationStartedNotification: "+emmigrationQueue);
		_dispatcher.addDestination(emmigrationQueue); 
	}
	
	public void onMessage(ObjectMessage om, EmmigrationEndedNotification sden) {
		Destination emmigrationQueue=sden.getDestination();
		_log.info("received EmmigrationEndedNotification: "+emmigrationQueue);
		_dispatcher.removeDestination(emmigrationQueue);
	}
	
	public void onMessage(ObjectMessage om, EmmigrationRequest er) throws JMSException {
		String id=er.getId();
		_log.info("receiving migration request: "+id);
		try {
			MessageDispatcher.Settings settingsInOut=new MessageDispatcher.Settings();
			// reverse direction...
			settingsInOut.to=om.getJMSReplyTo();
			settingsInOut.from=_dispatcher.getCluster().getLocalNode().getDestination();
			settingsInOut.correlationId=om.getJMSCorrelationID();
			_log.info("received EmmigrationRequest: "+id);
//			_top.demote(id, MOTABLE);
			_dispatcher.sendMessage(new EmmigrationAcknowledgement(id), settingsInOut);
		} catch (Exception e) {
			_log.warn("problem handling migration request: "+id, e);
		}
		// TODO - if we see a LocationRequest for a session that we know is Dead - we should respond immediately.
	}
	
	protected final HashMap _emmigrationRvMap=new HashMap();
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.context.Contextualiser#demote(java.lang.String, org.codehaus.wadi.sandbox.context.Motable)
	 */
	public void demote(String id, Motable val) {		
		if (_emmigrationQueue==null) {
			// pass straight through...
			_next.demote(id, val);
		} else {
			// push out to another node in the cluster
			MessageDispatcher.Settings settingsInOut=new MessageDispatcher.Settings();
			settingsInOut.to=_emmigrationQueue;
			settingsInOut.correlationId=id;
			settingsInOut.from=_dispatcher.getCluster().getLocalNode().getDestination();
			EmmigrationAcknowledgement ea=(EmmigrationAcknowledgement)_dispatcher.exchangeMessages(id, _emmigrationRvMap, new EmmigrationRequest(id, null), settingsInOut, 3000);
			_log.info("received EmmigrationAcknowledgement: "+ea.getId()+" ["+settingsInOut.to+"]");
			// we should :
			// remove the session our side - demotion API needs to work like promotion API
			// cache location of emmigrating session...
			// TODO
		}
	}

	public boolean isLocal(){return false;}
}