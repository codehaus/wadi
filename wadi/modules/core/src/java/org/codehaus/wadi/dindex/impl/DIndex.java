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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.activecluster.ClusterEvent;
import org.activecluster.ClusterListener;
import org.activecluster.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.dindex.CoordinatorConfig;
import org.codehaus.wadi.dindex.PartitionManager;
import org.codehaus.wadi.dindex.PartitionManagerConfig;
import org.codehaus.wadi.dindex.StateManager;
import org.codehaus.wadi.dindex.StateManagerConfig;
import org.codehaus.wadi.dindex.messages.DIndexDeletionRequest;
import org.codehaus.wadi.dindex.messages.DIndexForwardRequest;
import org.codehaus.wadi.dindex.messages.DIndexInsertionRequest;
import org.codehaus.wadi.dindex.messages.DIndexInsertionResponse;
import org.codehaus.wadi.dindex.messages.DIndexRelocationRequest;
import org.codehaus.wadi.dindex.newmessages.RelocationRequestI2P;
import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.gridstate.PartitionMapper;
import org.codehaus.wadi.gridstate.activecluster.ActiveClusterDispatcher;
import org.codehaus.wadi.impl.Quipu;

import EDU.oswego.cs.dl.util.concurrent.Latch;

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

	public DIndex(String nodeName, int numPartitions, long inactiveTime, Dispatcher dispatcher, Map distributedState, PartitionMapper mapper) {
		_nodeName=nodeName;
		_log=LogFactory.getLog(getClass().getName()+"#"+_nodeName);
		_inactiveTime=inactiveTime;
		_dispatcher=dispatcher;
		_cluster=((ActiveClusterDispatcher)_dispatcher).getCluster();
		_distributedState=distributedState;
		_partitionManager=new SimplePartitionManager(_dispatcher, numPartitions, _distributedState, this, mapper);
		_stateManager=new SimpleStateManager(_dispatcher, _inactiveTime);
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
		if (_log.isInfoEnabled()) _log.info("local state: " + keys);
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
		if (_log.isDebugEnabled()) _log.debug("node left: "+getNodeName(node));
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

	public boolean insert(String name) {
		try {
			ObjectMessage message=_dispatcher.createObjectMessage();
			message.setJMSReplyTo(_cluster.getLocalNode().getDestination());
			DIndexInsertionRequest request=new DIndexInsertionRequest(name);
			message.setObject(request);
			ObjectMessage reply=getPartition(name).exchange(message, request, _inactiveTime);
			return ((DIndexInsertionResponse)reply.getObject()).getSuccess();
		} catch (Exception e) {
		  _log.warn("problem inserting session key into DHT", e);
		  return false;
		}
	}

	public void remove(String name) {
		try {
			ObjectMessage message=_dispatcher.createObjectMessage();
			message.setJMSReplyTo(_cluster.getLocalNode().getDestination());
			DIndexDeletionRequest request=new DIndexDeletionRequest(name);
			message.setObject(request);
			getPartition(name).exchange(message, request, _inactiveTime);
		} catch (Exception e) {
		  _log.info("oops...", e);
		}
	}

	public void relocate(String name) {
		try {
			ObjectMessage message=_dispatcher.createObjectMessage();
			message.setJMSReplyTo(_cluster.getLocalNode().getDestination());
			DIndexRelocationRequest request=new DIndexRelocationRequest(name);
			message.setObject(request);
			getPartition(name).exchange(message, request, _inactiveTime);
		} catch (Exception e) {
		  _log.info("oops...", e);
		}
	}

    public ObjectMessage relocate(String sessionName, String nodeName, int concurrentRequestThreads, boolean shuttingDown, long timeout) throws Exception {
        ObjectMessage message=_dispatcher.createObjectMessage();
        message.setJMSReplyTo(_dispatcher.getLocalDestination());
        RelocationRequestI2P request=new RelocationRequestI2P(sessionName, nodeName, concurrentRequestThreads, shuttingDown);
        message.setObject(request);
        //getPartition(sessionName).onMessage(message, request);
        return forwardAndExchange(sessionName, message, request, timeout);
    }
    
	public ObjectMessage forwardAndExchange(String name, ObjectMessage message, RelocationRequestI2P request, long timeout) {
		try {
		  _log.trace("wrapping request");
			DIndexForwardRequest request2=new DIndexForwardRequest(request);
			message.setObject(request2);
			return getPartition(name).exchange(message, request2, timeout);
		} catch (JMSException e) {
		  _log.info("oops...", e);
			return null;
		}
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
}

