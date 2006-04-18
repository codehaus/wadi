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
package org.codehaus.wadi.dindex.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.activecluster.Cluster;
import org.apache.activecluster.ClusterEvent;
import org.apache.activecluster.ClusterListener;
import org.apache.activecluster.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.InvocationContext;
import org.codehaus.wadi.InvocationException;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.dindex.CoordinatorConfig;
import org.codehaus.wadi.dindex.PartitionManager;
import org.codehaus.wadi.dindex.PartitionManagerConfig;
import org.codehaus.wadi.dindex.StateManager;
import org.codehaus.wadi.dindex.StateManagerConfig;
import org.codehaus.wadi.dindex.newmessages.DeleteIMToPM;
import org.codehaus.wadi.dindex.newmessages.EvacuateIMToPM;
import org.codehaus.wadi.dindex.newmessages.InsertIMToPM;
import org.codehaus.wadi.dindex.newmessages.InsertPMToIM;
import org.codehaus.wadi.dindex.newmessages.MoveIMToPM;
import org.codehaus.wadi.dindex.newmessages.MoveIMToSM;
import org.codehaus.wadi.dindex.newmessages.MovePMToIM;
import org.codehaus.wadi.dindex.newmessages.MoveSMToIM;
import org.codehaus.wadi.gridstate.PartitionMapper;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Quipu;
import org.codehaus.wadi.impl.AbstractChainedEmoter;
import org.codehaus.wadi.impl.SimpleMotable;
import org.codehaus.wadi.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DIndex implements ClusterListener, CoordinatorConfig, SimplePartitionManager.Callback, StateManagerConfig {

	protected final static String _nodeNameKey="nodeName";
	protected final static String _partitionKeysKey="partitionKeys";
	protected final static String _timeStampKey="timeStamp";
	protected final static String _birthTimeKey="birthTime";
	protected final static String _correlationIDMapKey="correlationIDMap";

	protected final Map _distributedState;
	protected final Latch _coordinatorLatch=new Latch();
	protected final Object _coordinatorLock=new Object();
	protected final Dispatcher _dispatcher;
	protected final Cluster _cluster;
	protected final String _nodeName;
	protected final Log _log;
	protected final long _inactiveTime;
	protected final PartitionManager _partitionManager;
	protected final StateManager _stateManager;
	protected final Log _lockLog=LogFactory.getLog("org.codehaus.wadi.LOCKS");

	public DIndex(String nodeName, int numPartitions, long inactiveTime, Dispatcher dispatcher, Map distributedState, PartitionMapper mapper) {
		_nodeName=nodeName;
		_log=LogFactory.getLog(getClass().getName()+"#"+_nodeName);
		_inactiveTime=inactiveTime;
		_dispatcher=dispatcher;
		_cluster=_dispatcher.getCluster();
		_distributedState=distributedState;
		_partitionManager=new SimplePartitionManager(_dispatcher, numPartitions, _distributedState, this, mapper);
		_stateManager= new SimpleStateManager(_dispatcher, _inactiveTime);
	}

	protected Node _coordinatorNode;
	protected Coordinator _coordinator;
	protected PartitionManagerConfig _config;

	public void init(PartitionManagerConfig config) {
		_log.info("init-ing...");
		_config=config;
		_cluster.setElectionStrategy(new SeniorityElectionStrategy());
		_dispatcher.setClusterListener(this);
		_distributedState.put(_nodeNameKey, _nodeName);
		_distributedState.put(_correlationIDMapKey, new HashMap());
		_distributedState.put(_birthTimeKey, new Long(System.currentTimeMillis()));
		PartitionKeys keys=_partitionManager.getPartitionKeys();
		_distributedState.put(_partitionKeysKey, keys);
		_distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
		_partitionManager.init(config);
		_stateManager.init(this);
		_log.info("...init-ed");
	}

	public void start() throws Exception {
		_log.info("starting...");

		_partitionManager.start();

		_log.info("sleeping...");
		boolean isNotCoordinator=_coordinatorLatch.attempt(_inactiveTime); // wait to find out if we are the Coordinator
		_log.info("...waking");

		// If our wait timed out, then we must be the coordinator...
		if (!isNotCoordinator) {
			_partitionManager.localise();
			PartitionKeys k=_partitionManager.getPartitionKeys();
			_distributedState.put(_partitionKeysKey, k);
			_distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
			if (_log.isInfoEnabled()) _log.info("local state: " + k);
			_dispatcher.setDistributedState(_distributedState);
			if (_log.isTraceEnabled()) _log.trace("distributed state updated: " + _dispatcher.getDistributedState());
			onCoordinatorChanged(new ClusterEvent(_cluster, _cluster.getLocalNode(), ClusterEvent.ELECTED_COORDINATOR));
			_coordinator.queueRebalancing();
		}

		// whether we are the coordinator or not...

		_log.info("...started");
	}

	public void stop() throws Exception {
		_log.info("stopping...");

		Thread.interrupted();

		_stateManager.stop();

		if (_coordinator!=null) {
			_coordinator.stop();
			_coordinator=null;
		}

		_partitionManager.stop();

		_log.info("...stopped");
	}

	public Cluster getCluster() {
		return _cluster;
	}

	public Dispatcher getDispatcher() {
		return _dispatcher;
	}

	public PartitionManager getPartitionManager() {
		return _partitionManager;
	}

	// ClusterListener

	public int getPartition() {
		// TODO - think about synchronisation...
		PartitionKeys keys=(PartitionKeys)_distributedState.get(_partitionKeysKey);
		return keys.getKeys()[Math.abs((int)(Math.random()*keys.size()))];
	}

	public void onNodeUpdate(ClusterEvent event) {
		Node node=event.getNode();
		if (_log.isTraceEnabled()) _log.trace("onNodeUpdate: " + getNodeName(node) + ": " + node.getState());

		_partitionManager.update(node);

		Map state=node.getState();
		correlateStateUpdate(state);
	}

	protected void correlateStateUpdate(Map state) {
		Map correlationIDMap=(Map)state.get(_correlationIDMapKey);
		Destination local=_dispatcher.getLocalDestination();
		String correlationID=(String)correlationIDMap.get(local);
		if (correlationID!=null) {
			Quipu rv=(Quipu)_dispatcher.getRendezVousMap().get(correlationID);
			if (rv==null) {
				if (_log.isWarnEnabled()) _log.warn("no one waiting for: " + correlationID);
			} else {
				if (_log.isTraceEnabled()) _log.trace("successful correlation: " + correlationID);
				rv.putResult(state);
			}
		}
	}

	public void onNodeAdd(ClusterEvent event) {
		Node node=event.getNode();

		if (_log.isDebugEnabled()) _log.debug("node joined: "+getNodeName(node));

		if (_cluster.getLocalNode()==_coordinatorNode) {
			_coordinator.queueRebalancing();
		}

		_partitionManager.update(node);
	}

	public void onNodeRemoved(ClusterEvent event) {
		Node node=event.getNode();
		if (_log.isDebugEnabled()) _log.debug("node leaving: "+getNodeName(node));
		_leavers.add(node.getDestination());
		if (_coordinator!=null)
			_coordinator.queueRebalancing();
	}


	public boolean amCoordinator() {
		return _coordinatorNode.getDestination().equals(_dispatcher.getLocalDestination());
	}

	public void onNodeFailed(ClusterEvent event) {
		Node node=event.getNode();
		if (_log.isDebugEnabled()) _log.info("node failed: "+getNodeName(node));
		if (_leavers.remove(node.getDestination())) {
			// we have already been explicitly informed of this node's wish to leave...
			_left.remove(node);
			if (_log.isTraceEnabled()) _log.trace("onNodeFailed:" + getNodeName(node) + "- already evacuated - ignoring");
		} else {
			if (_log.isErrorEnabled()) _log.error("onNodeFailed: " + getNodeName(node));
			if (amCoordinator()) {
				if (_log.isErrorEnabled()) _log.error("CATASTROPHIC FAILURE on: " + getNodeName(node));
				if (_coordinator!=null)
					_coordinator.queueRebalancing();
				else
					_log.warn("coordinator thread not running");
			}
		}
	}

	public void onCoordinatorChanged(ClusterEvent event) {
		synchronized (_coordinatorLock) {
			if (_log.isDebugEnabled()) _log.debug("coordinator elected: " + getNodeName(event.getNode()));
			Node newCoordinator=event.getNode();
			if (newCoordinator!=_coordinatorNode) {
				if (_coordinatorNode==_cluster.getLocalNode())
					onDismissal(event);
				_coordinatorNode=newCoordinator;
				if (_coordinatorNode==_cluster.getLocalNode())
					onElection(event);
			}

			_coordinatorLatch.release(); // we are still waiting in start() to find out if we are the Coordinator...
		}
	}

	public Collection[] createResultSet(int numPartitions, int[] keys) {
		Collection[] c=new Collection[numPartitions];
		for (int i=0; i<keys.length; i++)
			c[keys[i]]=new ArrayList();
		return c;
	}

	public void onElection(ClusterEvent event) {
		_log.info("accepting coordinatorship");
		try {
			(_coordinator=new Coordinator(this)).start();
			_coordinator.queueRebalancing();
		} catch (Exception e) {
			_log.error("problem starting Coordinator");
		}
	}

	public void onDismissal(ClusterEvent event) {
		_log.info("resigning coordinatorship"); // never happens - coordinatorship is for life..
		try {
			_coordinator.stop();
			_coordinator=null;
		} catch (Exception e) {
			_log.error("problem starting Balancer");
		}
	}


	public static String getNodeName(Node node) {
		return node==null?"<unknown>":(String)node.getState().get(_nodeNameKey);
	}

	public boolean isCoordinator() {
		synchronized (_coordinatorLock) {
			return _cluster.getLocalNode()==_coordinatorNode;
		}
	}

	public Node getCoordinator() {
		synchronized (_coordinatorLock) {
			return _coordinatorNode;
		}
	}

	public int getNumPartitions() {
		return _partitionManager.getNumPartitions();
	}

	public Node getLocalNode() {
		return _cluster.getLocalNode();
	}

	public Collection getRemoteNodes() {
		return _cluster.getNodes().values();
	}

	public Map getRendezVousMap() {
		return _dispatcher.getRendezVousMap();
	}

	protected final Collection _leavers=Collections.synchronizedCollection(new ArrayList());
	protected final Collection _left=Collections.synchronizedCollection(new ArrayList());

	public Collection getLeavers() {
		return _leavers;
	}

	public Collection getLeft() {
		return _left;
	}

	protected int printNode(Node node) {
		if (node!=_cluster.getLocalNode())
			node=(Node)_cluster.getNodes().get(node.getDestination());
		if (node==null) {
			if (_log.isInfoEnabled()) _log.info(DIndex.getNodeName(node) + " : <unknown> - {?...}");
			return 0;
		} else {
			PartitionKeys keys=DIndex.getPartitionKeys(node);
			int amount=keys.size();
			if (_log.isInfoEnabled()) _log.info(DIndex.getNodeName(node) + " : " + amount + " - " + keys);
			return amount;
		}
	}
	// temporary test methods...

	public boolean insert(String name, long timeout) {
		try {
			InsertIMToPM request=new InsertIMToPM(name);
			PartitionFacade pf=getPartition(name);
			ObjectMessage reply=pf.exchange(request, timeout);
			return ((InsertPMToIM)reply.getObject()).getSuccess();
		} catch (Exception e) {
			_log.warn("problem inserting session key into DHT", e);
			return false;
		}
	}

	public void remove(String name) {
		try {
			DeleteIMToPM request=new DeleteIMToPM(name);
			getPartition(name).exchange(request, _inactiveTime);
		} catch (Exception e) {
			_log.info("oops...", e);
		}
	}

	public void relocate(String name) {
		try {
			EvacuateIMToPM request=new EvacuateIMToPM(name);
			getPartition(name).exchange(request, _inactiveTime);
		} catch (Exception e) {
			_log.info("oops...", e);
		}
	}

	class SMToIMEmoter extends AbstractChainedEmoter {
		protected final Log _log=LogFactory.getLog(getClass());

		protected final String _nodeName;
		protected final ObjectMessage _message;

		protected Sync _invocationLock;
		protected Sync _stateLock;

		public SMToIMEmoter(String nodeName, ObjectMessage message) {
			_nodeName=nodeName;
			_message=message;
		}

		public boolean prepare(String name, Motable emotable, Motable immotable) {
			try {
//				Sync _stateLock=((Context)emotable).getExclusiveLock();
//				if (_lockLog.isTraceEnabled()) _lockLog.trace("State - (excl.): "+name+ " ["+Thread.currentThread().getName()+"]");
//				Utils.acquireUninterrupted(_stateLock);
//				if (_lockLog.isTraceEnabled()) _lockLog.trace("State - (excl.): "+name+ " ["+Thread.currentThread().getName()+"]");
				immotable.copy(emotable);
			} catch (Exception e) {
				_log.warn("oops", e);
				return false;
			}

			return true;
		}

		public void commit(String name, Motable emotable) {
			try {
				// respond...
				MoveIMToSM response=new MoveIMToSM(true);
				_dispatcher.reply(_message, response);

				emotable.destroy(); // remove copy in store
//				if (_lockLog.isTraceEnabled()) _lockLog.trace("State (excl.) - releasing: "+name+ " ["+Thread.currentThread().getName()+"]");
//				_stateLock.release();
//				if (_lockLog.isTraceEnabled()) _lockLog.trace("State (excl.) - released: "+name+ " ["+Thread.currentThread().getName()+"]");

			} catch (Exception e) {
				throw new UnsupportedOperationException("NYI"); // NYI
			}
		}

		public void rollback(String name, Motable motable) {
			throw new RuntimeException("NYI");
		}

		public String getInfo() {
			return "immigration:"+_nodeName;
		}
	}

  public boolean fetchSession(Object key) {
    boolean success=false;
    try {
      Motable motable=relocate((String)key, getLocalNodeName(), 0, false, 5000L);
      success=true;
    } catch (Exception e) {
      _log.error("unexpected problem", e);
    }
    return success;
  }

	public Motable relocate(String sessionName, String nodeName, int concurrentRequestThreads, boolean shuttingDown, long timeout) throws Exception {
		MoveIMToPM request=new MoveIMToPM(sessionName, nodeName, concurrentRequestThreads, shuttingDown);
		ObjectMessage message=getPartition(sessionName).exchange(request, timeout);

		if (message==null) {
			_log.error("something went wrong - what should we do?"); // TODO
			return null;
		}

		try {
			Serializable dm=(Serializable)message.getObject();
			// the possibilities...
			if (dm instanceof MoveSMToIM) {
				MoveSMToIM req=(MoveSMToIM)dm;
				// insert motable into contextualiser stack...
				byte[] bytes=(byte[])req.getValue();
				if (bytes==null) {
					_log.warn("failed relocation - 0 bytes arrived: "+sessionName);
					return null;
				} else {
					Motable emotable=new SimpleMotable();
					emotable.setBodyAsByteArray(bytes);
					// TOTAL HACK - FIXME
					emotable.setLastAccessedTime(System.currentTimeMillis());
					if (!emotable.checkTimeframe(System.currentTimeMillis()))
						if (_log.isWarnEnabled()) _log.warn("immigrating session has come from the future!: "+emotable.getName());

					Emoter emoter=new SMToIMEmoter(_config.getNodeName(message.getJMSReplyTo()), message);
					Immoter immoter=_config.getImmoter(sessionName, emotable);
					Motable immotable=Utils.mote(emoter, immoter, emotable, sessionName);
					return immotable;
//					if (null==immotable)
//					return false;
//					else {
//					boolean answer=immoter.contextualise(null, null, null, sessionName, immotable, null);
//					return answer;
//					}
				}
			} else if (dm instanceof MovePMToIM) {
				if (_log.isTraceEnabled()) _log.trace("unknown session: "+sessionName);
				return null;
			} else {
				_log.warn("unexpected response returned - what should I do? : "+dm);
				return null;
			}
		} catch (JMSException e) {
			_log.warn("could not extract message body", e);
		}
		return null;
	}

	public PartitionFacade getPartition(Object key) {
		return _partitionManager.getPartition(key);
	}

	public String getNodeName(Destination destination) {
		Node local=_cluster.getLocalNode();
		Node node=destination.equals(local.getDestination())?local:(Node)_cluster.getNodes().get(destination);
		return getNodeName(node);
	}

	public long getInactiveTime() {
		return _inactiveTime;
	}

	public void regenerateMissingPartitions(Node[] living, Node[] leaving) {
		_partitionManager.regenerateMissingPartitions(living, leaving);
	}

	public static PartitionKeys getPartitionKeys(Node node) {
		return ((PartitionKeys)node.getState().get(_partitionKeysKey));
	}


	// only for use whilst developing GridState...

//	public PartitionFacade[] getPartitions() {
//	return _partitions;
//	}

	// StateManagerConfig API

	public PartitionFacade getPartition(int key) {
		return _partitionManager.getPartition(key);
	}

	public StateManager getStateManager() {
		return _stateManager;
	}

	// StateManagerConfig API

	public String getLocalNodeName() {
		return _nodeName;
	}

	public 	boolean contextualise(InvocationContext invocationContext, String id, Immoter immoter, Sync motionLock, boolean exclusiveOnly) throws InvocationException {
		return _config.contextualise(invocationContext, id, immoter, motionLock, exclusiveOnly);
	}

	public Sync getInvocationLock(String name) {
		return _config.getInvocationLock(name);
	}

}
