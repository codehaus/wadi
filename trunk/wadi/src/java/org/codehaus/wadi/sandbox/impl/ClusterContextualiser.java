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
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.wadi.sandbox.Cluster;
import org.codehaus.wadi.sandbox.Collapser;
import org.codehaus.wadi.sandbox.Contextualiser;
import org.codehaus.wadi.sandbox.Emoter;
import org.codehaus.wadi.sandbox.Evicter;
import org.codehaus.wadi.sandbox.Immoter;
import org.codehaus.wadi.sandbox.Location;
import org.codehaus.wadi.sandbox.Motable;
import org.codehaus.wadi.sandbox.RelocationStrategy;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

// this needs to be split into several parts...

// (1) a request relocation strategy (proxy)
// (2) a state relocation strategy (migrate) - used if (1) fails
// a location cache - used by both the above when finding the locarion of state...

// demotion :

// if the last node in the cluster, pass demotions through to e.g. shared DB below us.
// else, distribute them out to the cluster

// on startup, e.g. db will read in complete complement of sessions and try to promote them
// how do we promote them to disc, but not to memory ? - perhaps in the configuration as when the node shutdown ?
// could we remember the ttl and set it up the same, so nothing times out whilst the cluster is down ?

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
public class ClusterContextualiser extends AbstractSharedContextualiser {

	protected final HashMap _emigrationRvMap=new HashMap();
	protected final Cluster _cluster;
	protected final MessageDispatcher _dispatcher;
	protected final RelocationStrategy _relocater;
	protected final Location _location;
	protected final Immoter _immoter;
	protected final Emoter _emoter;
    protected final int _ackTimeout=500; // TODO - parameterise
    protected final Map _map;
    protected final Evicter _evicter;

	/**
	 * @param next
	 * @param evicter
	 * @param map
	 * @param location TODO
	 */
	public ClusterContextualiser(Contextualiser next, Collapser collapser, Evicter evicter, Map map, Cluster cluster, MessageDispatcher dispatcher, RelocationStrategy relocater, Location location) {
		super(next, new CollapsingLocker(collapser));
		_cluster=cluster;
		_dispatcher=dispatcher;
	    _relocater=relocater;
	    _location=location;
        _map=map;
        _evicter=evicter;

	    _immoter=new EmigrationImmoter();
	    _emoter=null; // TODO - I think this should be something like the ImmigrationEmoter
	    // it pulls a named Session out of the cluster and emotes it from this Contextualiser...
	    // this makes it awkward to split session and request relocation into different strategies,
	    // so session relocation should be the basic strategy, with request relocation as a pluggable
	    // optimisation...

	    _dispatcher.register(this, "onMessage");
	    _dispatcher.register(EmigrationAcknowledgement.class, _emigrationRvMap, _ackTimeout);

	    if (_log.isTraceEnabled()) _log.trace("Destination is: "+_cluster.getLocalNode().getDestination());
		}

    public Immoter getImmoter(){return _immoter;}
    public Emoter getEmoter(){return _emoter;}

    public Immoter getDemoter(String id, Motable motable) {
        if (_cluster.getNodes().size()>=1) {
            ensureEmmigrationQueue();
            return getImmoter();
        } else {
            return _next.getDemoter(id, motable);
        }
    }

    public Immoter getSharedDemoter() {
        if (_cluster.getNodes().size()>=1) {
            ensureEmmigrationQueue();
            return getImmoter();
        } else {
            return _next.getSharedDemoter();
        }
    }

	// this field forms part of a circular dependency - so we need a setter rather than ctor param
	protected Contextualiser _top;
	public Contextualiser getTop() {return _top;}
	public void setTop(Contextualiser top) {_top=top;}

	public boolean handle(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Immoter immoter, Sync motionLock) throws IOException, ServletException {
		return _relocater.relocate(hreq, hres, chain, id, immoter, motionLock, _map);
	}

	// be careful here - there are two things going on, a map of cached locations, which needs management
	// and incoming sessions which must be routed either out to cluster, or down to e.g. DB...
	public void evict() {
		// how long do we wish to maintain cached Locations ?
		// TODO - get timestamps working on Locations - write a hybrid Evicter ?
		// or implement Chained instead of Mapped Contextualiser - consider....
	}

	protected Destination _emigrationQueue;

    protected void createEmigrationQueue() throws JMSException {
        _log.trace("creating emigration queue");
        _emigrationQueue=_cluster.createQueue("EMIGRATION"); // TODO - better queue name ?
        MessageDispatcher.Settings settings=new MessageDispatcher.Settings();
        settings.to=_dispatcher.getCluster().getDestination();
        _dispatcher.sendMessage(new EmigrationStartedNotification(_emigrationQueue), settings);
    }

    protected void destroyEmigrationQueue() throws JMSException {
        MessageDispatcher.Settings settings=new MessageDispatcher.Settings();
        settings.to=_dispatcher.getCluster().getDestination();
        _dispatcher.sendMessage(new EmigrationEndedNotification(_emigrationQueue), settings);
        // FIXME - can we destroy the queue ?
        _log.trace("emigration queue destroyed");
    }

	protected synchronized void ensureEmmigrationQueue() {
	    try {
	        if (_emigrationQueue==null) {
                createEmigrationQueue();
	        }
	    } catch (JMSException e) {
	        _log.error("emmigration queue initialisation failed", e);
	        _emigrationQueue=null;
	    }
	}

    public void stop() throws Exception {
        if (_emigrationQueue!=null) { // evacuation is synchronous, so we will not get to here until all sessions are gone...
            destroyEmigrationQueue();
        }
        super.stop();
    }

	public void onMessage(ObjectMessage om, EmigrationStartedNotification sdsn) throws JMSException {
		Destination emigrationQueue=sdsn.getDestination();
		if (_log.isTraceEnabled()) _log.trace("received EmigrationStartedNotification: "+emigrationQueue);
		_dispatcher.addDestination(emigrationQueue);
	}

	public void onMessage(ObjectMessage om, EmigrationEndedNotification sden) {
		Destination emigrationQueue=sden.getDestination();
		if (_log.isTraceEnabled()) _log.trace("received EmigrationEndedNotification: "+emigrationQueue);
		_dispatcher.removeDestination(emigrationQueue);
	}

	/**
	 * Manage the immotion of a session into the cluster tier from another and its emigration thence to another node.
	 *
	 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
	 * @version $Revision$
	 */
	class EmigrationImmoter implements Immoter {
		public Motable nextMotable(String id, Motable emotable) {return new SimpleMotable();}

		public boolean prepare(String id, Motable emotable, Motable immotable) {
		    MessageDispatcher.Settings settingsInOut=new MessageDispatcher.Settings();
		    settingsInOut.to=_emigrationQueue;
		    settingsInOut.correlationId=id;
		    settingsInOut.from=_cluster.getLocalNode().getDestination();
		    try {
		        immotable.copy(emotable);
		        EmigrationRequest er=new EmigrationRequest(id, immotable);
		        EmigrationAcknowledgement ea=(EmigrationAcknowledgement)_dispatcher.exchangeMessages(id, _emigrationRvMap, er, settingsInOut, _ackTimeout);

		        if (ea==null) {
		            return false;
		        } else {
		            _map.put(id, ea.getLocation()); // cache new Location of Session
		            return true;
		        }
		    } catch (Exception e) {
		        if (_log.isWarnEnabled()) _log.warn("problem sending emigration request: "+id, e);
		        return false;
		    }
		}

		public void commit(String id, Motable immotable) {
			// TODO - cache new location of emigrating session...
			}

		public void rollback(String id, Motable immotable) {
			// TODO - errr... HOW ?
			}

		public boolean contextualise(HttpServletRequest hreq, HttpServletResponse hres, FilterChain chain, String id, Motable immotable, Sync motionLock) {
            return false;
            // TODO - perhaps this is how a proxied contextualisation should occur ?
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
	class ImmigrationEmoter implements Emoter {

		protected final ObjectMessage _om;
		protected final EmigrationRequest _er;
		protected final MessageDispatcher.Settings _settingsInOut;

		public ImmigrationEmoter(ObjectMessage om, EmigrationRequest er) {
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
				if (_log.isErrorEnabled()) _log.error("could not acknowledge safe receipt: "+id, e);
			}

		}

		public void rollback(String id, Motable emotable) {
			throw new RuntimeException("NYI");
			// difficult !!!
		}

		public String getInfo() {
			return "cluster";
		}
	}

	public void onMessage(ObjectMessage om, EmigrationRequest er) {
        String id=er.getId();
        if (_log.isTraceEnabled()) _log.trace("EmigrationRequest received: "+id);
        Sync lock=_locker.getLock(id, null);
        boolean acquired=false;
        try {
            Utils.acquireUninterrupted(lock);
            acquired=true;

            Emoter emoter=new ImmigrationEmoter(om, er);
            Motable emotable=er.getMotable();

            if (!emotable.checkTimeframe(System.currentTimeMillis()))
                if (_log.isWarnEnabled()) _log.warn("immigrating session has come from the future!: "+emotable.getId());

            Immoter immoter=_top.getDemoter(id, emotable);
            Utils.mote(emoter, immoter, emotable, id);
        } catch (TimeoutException e) {
            if (_log.isWarnEnabled()) _log.warn("could not acquire promotion lock for incoming session: "+id);
        } finally {
            if (acquired)
                lock.release();
        }
	}

    // another hack, because this should not inherit from Mapped...
    public int loadMotables(Emoter emoter, Immoter immoter){return 0;}

    // AbstractMotingContextualiser
    public Motable get(String id) {return (Motable)_map.get(id);}

    // EvicterConfig
    // BestEffortEvicters
    public Map getMap() {return _map;}

    public Emoter getEvictionEmoter() {throw new UnsupportedOperationException();} // FIXME
    public void expire(Motable motable) {throw new UnsupportedOperationException();} // FIXME

}
