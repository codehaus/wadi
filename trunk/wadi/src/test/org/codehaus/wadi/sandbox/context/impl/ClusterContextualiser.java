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
import org.codehaus.wadi.sandbox.context.Emoter;
import org.codehaus.wadi.sandbox.context.Immoter;
import org.codehaus.wadi.sandbox.context.Location;
import org.codehaus.wadi.sandbox.context.Motable;
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

	protected final HashMap _emigrationRvMap=new HashMap();
	protected final MessageDispatcher _dispatcher;
	protected final RelocationStrategy _relocater;
	protected final Location _location;
	protected final Immoter _immoter;
	protected final Emoter _emoter;

	/**
	 * @param next
	 * @param collapser
	 * @param map
	 * @param evicter
	 * @param location TODO
	 */
	public ClusterContextualiser(Contextualiser next, Collapser collapser, Map map, SwitchableEvicter evicter, MessageDispatcher dispatcher, RelocationStrategy relocater, Location location) throws JMSException {
		super(next, collapser, map, evicter);
		_dispatcher=dispatcher;
	    _relocater=relocater;
	    _location=location;
	    
	    _immoter=new ClusterImmoter();
	    _emoter=null; // TODO - I think this should be something like the ImmigrationEmoter
	    // it pulls a names Session out of the cluster and emotes it from this Contextualiser...
	    // this makes it awkward to split session and request relocation into different strategies,
	    // so session relocation should be the basic strategy, with request relocation as a pluggable
	    // optimisation...
	    
	    _dispatcher.register(this, "onMessage");
	    _dispatcher.register(EmigrationAcknowledgement.class, _emigrationRvMap, 3000);
		}
	
	public Immoter getImmoter(){return _immoter;}
	public Emoter getEmoter(){return _emoter;}
	
	protected Contextualiser _top;
	public Contextualiser getTop() {return _top;}
	public void setTop(Contextualiser top) {_top=top;}
	
	public boolean contextualiseLocally(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync promotionLock) throws IOException, ServletException {
		return _relocater.relocate(hreq, hres, chain, id, immoter, promotionLock, _map);
	}

	public boolean contextualiseLocally(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Sync promotionLock, Motable motable) throws IOException, ServletException {
		// this should delegate...
		throw new RuntimeException("NYI");
	}

	public void evict() {
		// how long do we wish to maintain cached Locations ?
	}

	public boolean isLocal(){return false;}

	protected Destination _emigrationQueue;

	public void setEmigrationQueue(Destination emigrationQueue) throws Exception {
		_emigrationQueue=emigrationQueue;
		((SwitchableEvicter)_evicter).setSwitch(false);
		// send message inviting everyone to start reading from queue
		MessageDispatcher.Settings settings=new MessageDispatcher.Settings();
		settings.to=_dispatcher.getCluster().getDestination();
		_dispatcher.sendMessage(new EmigrationStartedNotification(_emigrationQueue), settings);
//		Thread.sleep(1000);
//		_dispatcher.sendMessage(new ShutDownEndedNotification(_emigrationQueue), settings);
		// dump subsequent demoted Contexts onto queue
		}
	
	public void onMessage(ObjectMessage om, EmigrationStartedNotification sdsn) throws JMSException {
		Destination emigrationQueue=sdsn.getDestination();
		//_log.info("received EmigrationStartedNotification: "+emigrationQueue);
		_dispatcher.addDestination(emigrationQueue); 
	}
	
	public void onMessage(ObjectMessage om, EmigrationEndedNotification sden) {
		Destination emigrationQueue=sden.getDestination();
		//_log.info("received EmigrationEndedNotification: "+emigrationQueue);
		_dispatcher.removeDestination(emigrationQueue);
	}
	
	public Immoter getPromoter(Immoter immoter) {
		return immoter; // TODO - would we ever want to allow promotion of a context into our cache or out to the cluster ?
	}
	
	/**
	 * Manage the immotion of a session into the cluster tier from another and its emigration thence to another node.
	 *
	 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
	 * @version $Revision$
	 */
	class ClusterImmoter implements Immoter {
		public Motable nextMotable(String id, Motable emotable) {return new SimpleMotable();}
		
		public boolean prepare(String id, Motable emotable, Motable immotable) {
			MessageDispatcher.Settings settingsInOut=new MessageDispatcher.Settings();
			settingsInOut.to=_emigrationQueue;
			settingsInOut.correlationId=id;
			settingsInOut.from=_dispatcher.getCluster().getLocalNode().getDestination();
			EmigrationRequest er=new EmigrationRequest(id, emotable);
			EmigrationAcknowledgement ea=(EmigrationAcknowledgement)_dispatcher.exchangeMessages(id, _emigrationRvMap, er, settingsInOut, 3000);
			
			_map.put(id, ea.getLocation()); // cache new Location of Session
			return ea!=null;
		}
		
		public void commit(String id, Motable immotable) {
			// TODO - cache new location of emigrating session...
			}
		
		public void rollback(String id, Motable immotable) {
			// TODO - errr... HOW ?
			}
		
		public void contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Motable immotable) throws IOException, ServletException {
			// TODO
			//contextualiseLocally(hreq, hres, chain, id, new ClusterImmoter(), new NullSync(), motable);
		}
		
		public String getInfo() {
			return "cluster";
		}
	}

	/**
	 * Manage the immigration of a session from another node and and thence its emotion from the cluster layer into another.
	 *
	 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
	 * @version $Revision$
	 */
	class ClusterEmoter implements Emoter {
		
		protected final ObjectMessage _om;
		protected final EmigrationRequest _er;
		protected final MessageDispatcher.Settings _settingsInOut;
		
		public ClusterEmoter(ObjectMessage om, EmigrationRequest er) {
			_om=om;
			_er=er;
			_settingsInOut=new MessageDispatcher.Settings();
		}
		
		public boolean prepare(String id, Motable emotable, Motable immotable) {
			try {
				// reverse direction...
				_settingsInOut.to=_om.getJMSReplyTo();
				_settingsInOut.from=_dispatcher.getCluster().getLocalNode().getDestination();
				_settingsInOut.correlationId=_om.getJMSCorrelationID();
				return true;
			} catch (JMSException e) {
				return false;
			}
		}
		
		public void commit(String id, Motable emotable) {
			try {
				EmigrationAcknowledgement ea=new EmigrationAcknowledgement();
				ea.setId(id);
				ea.setLocation(_location);
				_dispatcher.sendMessage(ea, _settingsInOut);
			} catch (JMSException e) {
				_log.error("could not acknowledge safe receipt: "+id, e);
			}
			
			//_log.info("immigration (cluster): "+id);
		}
		
		public void rollback(String id, Motable emotable) {
			throw new RuntimeException("NYI");
			// difficult !!!
		}
		
		public String getInfo() {
			return "cluster";
		}
	}
	
	public void onMessage(ObjectMessage om, EmigrationRequest er) throws JMSException {
		String id=er.getId();
		Emoter emoter=new ClusterEmoter(om, er);
		Motable emotable=er.getMotable();
		Immoter immoter=_top.getDemoter(id, emotable);
		Utils.mote(emoter, immoter, emotable, id);
	}
}