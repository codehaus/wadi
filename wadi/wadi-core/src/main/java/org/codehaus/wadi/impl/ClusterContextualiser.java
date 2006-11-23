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

import java.util.Iterator;
import java.util.Set;

import org.codehaus.wadi.ClusteredContextualiserConfig;
import org.codehaus.wadi.Collapser;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.ContextualiserConfig;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Invocation;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.Relocater;
import org.codehaus.wadi.RelocaterConfig;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.location.StateManager;
import org.codehaus.wadi.location.impl.DIndex;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class ClusterContextualiser extends AbstractSharedContextualiser implements RelocaterConfig, ClusterListener, StateManager.ImmigrationListener {
    private final Collapser _collapser;
    private final Relocater _relocater;
    private final Immoter _immoter;
    private final Emoter _emoter;
    private final int _resTimeout = 5000; // TODO - parameterise
    private SynchronizedBoolean _shuttingDown;
    private String _nodeName;
    private Dispatcher _dispatcher;
    private Cluster _cluster;
    private DIndex _dindex;
    private Contextualiser _top;

	public ClusterContextualiser(Contextualiser next, Collapser collapser, Relocater relocater) {
		super(next, new CollapsingLocker(collapser), false);
		_collapser = collapser;
        _relocater = relocater;
        _immoter = new EmigrationImmoter();
        _emoter = null;
        // TODO - I think this should be something like the ImmigrationEmoter
		// it pulls a named Session out of the cluster and emotes it from this Contextualiser...
		// this makes it awkward to split session and request relocation into different strategies,
		// so session relocation should be the basic strategy, with request relocation as a pluggable
		// optimisation...
	}

	public void init(ContextualiserConfig config) {
		super.init(config);
		ClusteredContextualiserConfig ccc=(ClusteredContextualiserConfig)config;
		_shuttingDown=ccc.getShuttingDown();
		_nodeName=ccc.getNodeName();
		_dispatcher=ccc.getDispatcher();
		_cluster=_dispatcher.getCluster();
		_dindex=ccc.getDIndex();
		_top=ccc.getContextualiser();
		_relocater.init(this);
	}

	public Immoter getImmoter(){return _immoter;}
	public Emoter getEmoter(){return _emoter;}

	public Immoter getDemoter(String name, Motable motable) {
	    // how many partitions are we responsible for ?
	    // TODO - involves an unecessary allocation
	    if (_dindex.getPartitionManager().getBalancingInfo().getNumberOfLocalPartitionInfos() == 0) { 
	        // evacuate sessions to their respective partition masters...
	        return getImmoter();
	    } else {
	        return _next.getDemoter(name, motable);
	    }
	}
	
	public Immoter getSharedDemoter() {
	    // how many partitions are we responsible for ?
	    // TODO - involves an unecessary allocation
	    if (_dindex.getPartitionManager().getBalancingInfo().getNumberOfLocalPartitionInfos() == 0) {
	        // evacuate sessions to their respective partition masters...
	        return getImmoter();
	    } else {
	        // we must be the last node, push the sessions downwards into shared store...
	        return _next.getSharedDemoter();
	    }
	}

	public boolean handle(Invocation invocation, String id, Immoter immoter, Sync motionLock) throws InvocationException {
		return _relocater.relocate(invocation, id, immoter, motionLock);
	}

    public void start() throws Exception {
        super.start();
        _cluster.addClusterListener(this);
        _dindex.getStateManager().setImmigrationListener(this);
    }
    
	public void stop() throws Exception {
		super.stop();
	}

	/**
	 * Manage the immotion of a session into the cluster tier from another and its emigration thence to another node.
	 */
	class EmigrationImmoter implements Immoter {
		public Motable nextMotable(String id, Motable emotable) {return new SimpleMotable();}

		public boolean prepare(String name, Motable emotable, Motable immotable) {
			try {
				immotable.copy(emotable);
			} catch (Exception e) {
				if (_log.isWarnEnabled()) _log.warn("problem sending emigration request: "+name, e);
				return false;
			}
			return _dindex.getStateManager().offerEmigrant(name, immotable, _resTimeout);
		}

		public void commit(String name, Motable immotable) {
			// TODO - cache new location of emigrating session...
		}

		public void rollback(String name, Motable immotable) {
			// TODO - errr... HOW ?
		}

		public boolean contextualise(Invocation invocation, String id, Motable immotable, Sync motionLock) throws InvocationException {
			return false;
			// TODO - perhaps this is how a proxied contextualisation should occur ?
		}

	}

	/**
	 * Manage the immigration of a session from another node and and thence its emotion from the cluster layer into another.
	 *
	 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
	 * @version $Revision$
	 */
	class ImmigrationEmoter extends AbstractChainedEmoter {

		protected final Envelope _message;

		public ImmigrationEmoter(Envelope message) {
			_message=message;
		}

		public boolean prepare(String name, Motable emotable, Motable immotable) {
			if (super.prepare(name, emotable, immotable)) {
				_dindex.getStateManager().acceptImmigrant(_message, name, emotable);
				return true;
			} else {
				return false;
			}
		}

		public void commit(String name, Motable emotable) {
		}

		public void rollback(String name, Motable emotable) {
			throw new RuntimeException("NYI");
		}

	}

	public void onImmigration(Envelope message, Motable emotable) {
		String name=emotable.getName();
		if (_log.isTraceEnabled()) _log.trace("EmigrationRequest received: "+name);
		Sync invocationLock=_locker.getLock(name, emotable);
		boolean invocationLockAcquired=false;
		try {
			Utils.acquireUninterrupted("Invocation(ClusterContextualiser)", name, invocationLock);
			invocationLockAcquired=true;

			Emoter emoter=new ImmigrationEmoter(message);

			if (!emotable.checkTimeframe(System.currentTimeMillis())) {
			    _log.warn("immigrating session has come from the future!: "+emotable.getName());
            }

			Immoter immoter=_top.getDemoter(name, emotable);
			Utils.mote(emoter, immoter, emotable, name);
			notifySessionRelocation(name);
		} catch (TimeoutException e) {
			if (_log.isWarnEnabled()) _log.warn("could not acquire promotion lock for incoming session: "+name);
		} finally {
			if (invocationLockAcquired) {
				Utils.release("Invocation", name, invocationLock);
			}
		}
	}

	public void load(Emoter emoter, Immoter immoter) {
		// currently - we don't load anything from the Cluster - we could do state-balancing here
		// i.e. on startup, take ownership of a number of active sessions - affinity implications etc...
	}

    public void onListenerRegistration(Cluster cluster, Set existing, Peer coordinator) {
    }
	
    public void onMembershipChanged(Cluster cluster, Set joiners, Set leavers, Peer coordinator) {
        for (Iterator i=joiners.iterator(); i.hasNext() ;) {
            Peer peer=(Peer)i.next();
            if (_log.isTraceEnabled()) _log.trace("peer joined: " + peer);
        }
        for (Iterator i=leavers.iterator(); i.hasNext() ;) {
            Peer peer=(Peer)i.next();
            if (_log.isTraceEnabled()) _log.trace("peer left: " + peer);
        }
    }

	protected int _locationMaxInactiveInterval=30;

	// RelocaterConfig

	public Collapser getCollapser() {return _collapser;}
	public Dispatcher getDispatcher() {return _dispatcher;}
	public Contextualiser getContextualiser() {return _top;}
	public String getNodeName() {return _nodeName;}
	public SynchronizedBoolean getShuttingDown() {return _shuttingDown;}

	public DIndex getDIndex() {
		return _dindex;
	}

	public void notifySessionRelocation(String name) {
		_config.notifySessionRelocation(name);
	}

	public Motable get(String name) {
		throw new UnsupportedOperationException();
	}
	
	public String toString() {
		return "ClusterContextualiser [" + _cluster + "]";
	}

}
