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
import org.codehaus.wadi.dindex.BucketConfig;
import org.codehaus.wadi.dindex.CoordinatorConfig;
import org.codehaus.wadi.dindex.DIndexConfig;
import org.codehaus.wadi.dindex.DIndexRequest;
import org.codehaus.wadi.dindex.PartitionManager;
import org.codehaus.wadi.impl.Dispatcher;
import org.codehaus.wadi.impl.Quipu;

import EDU.oswego.cs.dl.util.concurrent.Latch;

public class DIndex implements ClusterListener, CoordinatorConfig, BucketConfig, SimplePartitionManager.Callback {

    protected final static String _nodeNameKey="nodeName";
    protected final static String _bucketKeysKey="bucketKeys";
    protected final static String _timeStampKey="timeStamp";
    protected final static String _birthTimeKey="birthTime";
    protected final static String _correlationIDMapKey="correlationIDMap";

    protected final Map _distributedState;
    protected final Latch _coordinatorLatch=new Latch();
    protected final Object _coordinatorLock=new Object();
    protected final Dispatcher _dispatcher;
    protected final String _nodeName;
    protected final Log _log;
    protected final int _numPartitions;
    protected final long _inactiveTime;
    protected final Cluster _cluster;
    protected final PartitionManager _partitionManager;

    public DIndex(String nodeName, int numPartitions, long inactiveTime, Cluster cluster, Dispatcher dispatcher, Map distributedState) {
        _nodeName=nodeName;
        _log=LogFactory.getLog(getClass().getName()+"#"+_nodeName);
        _numPartitions=numPartitions;
        _cluster=cluster;
        _inactiveTime=inactiveTime;
        _dispatcher=dispatcher;
        _distributedState=distributedState;
        _partitionManager=new SimplePartitionManager(_nodeName, _numPartitions, this, _cluster, _dispatcher, _distributedState, _inactiveTime, this);
    }

    protected Node _coordinatorNode;
    protected Coordinator _coordinator;
    protected DIndexConfig _config;

    public void init(DIndexConfig config) {
        _log.info("init-ing...");
        _config=config;
        _cluster.setElectionStrategy(new SeniorityElectionStrategy());
        _cluster.addClusterListener(this);
        _distributedState.put(_nodeNameKey, _nodeName);
        _distributedState.put(_correlationIDMapKey, new HashMap());
        _distributedState.put(_birthTimeKey, new Long(System.currentTimeMillis()));
        BucketKeys keys=_partitionManager.getPartitionKeys();
        _distributedState.put(_bucketKeysKey, keys);
        _distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
        _log.info("local state: "+keys);
        _partitionManager.init(config);
        _dispatcher.register(this, "onDIndexInsertionRequest", DIndexInsertionRequest.class);
        _dispatcher.register(DIndexInsertionResponse.class, _inactiveTime);
        _dispatcher.register(this, "onDIndexDeletionRequest", DIndexDeletionRequest.class);
        _dispatcher.register(DIndexDeletionResponse.class, _inactiveTime);
        _dispatcher.register(this, "onDIndexRelocationRequest", DIndexRelocationRequest.class);
        _dispatcher.register(DIndexRelocationResponse.class, _inactiveTime);
        _dispatcher.register(this, "onDIndexForwardRequest", DIndexForwardRequest.class);
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
            BucketKeys k=_partitionManager.getPartitionKeys();
            _distributedState.put(_bucketKeysKey, k);
            _distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
            _log.info("local state: "+k);
            _cluster.getLocalNode().setState(_distributedState);
            _log.trace("distributed state updated: "+_cluster.getLocalNode().getState());
            onCoordinatorChanged(new ClusterEvent(_cluster, _cluster.getLocalNode(), ClusterEvent.ELECTED_COORDINATOR));
            _coordinator.queueRebalancing();
        }

        // whether we are the coordinator or not...
        _partitionManager.dequeue();
        
        _log.info("...started");
    }

    public void stop() throws Exception {
        _log.info("stopping...");

        Thread.interrupted();

        BucketEvacuationRequest request=new BucketEvacuationRequest();
        Node localNode=_cluster.getLocalNode();
        String correlationId=_cluster.getLocalNode().getName();
        while (_dispatcher.exchangeSend(localNode.getDestination(), _coordinatorNode.getDestination(), correlationId, request, _inactiveTime)==null) {
        	_log.warn("could not contact Coordinator - backing off for "+_inactiveTime+" millis...");
        	Thread.sleep(_inactiveTime);
        }

        _dispatcher.deregister("onDIndexInsertionRequest", DIndexInsertionRequest.class, 5000);
        _dispatcher.deregister("onDIndexDeletionRequest", DIndexDeletionRequest.class, 5000);
        _dispatcher.deregister("onDIndexRelocationRequest", DIndexRelocationRequest.class, 5000);
        _dispatcher.deregister("onDIndexForwardRequest", DIndexForwardRequest.class, 5000);

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

    // ClusterListener

    public int getBucket() {
    	// TODO - think about synchronisation...
    	BucketKeys keys=(BucketKeys)_distributedState.get(_bucketKeysKey);
    	return keys.getKeys()[Math.abs((int)(Math.random()*keys.size()))];
    }

    public void onNodeUpdate(ClusterEvent event) {
        Node node=event.getNode();
        _log.info("onNodeUpdate: "+getNodeName(node)+": "+node.getState());

        _partitionManager.update(node);
        
        Map state=node.getState();
        correlateStateUpdate(state);
    }

    protected void correlateStateUpdate(Map state) {
        Map correlationIDMap=(Map)state.get(_correlationIDMapKey);
        Destination local=_cluster.getLocalNode().getDestination();
        String correlationID=(String)correlationIDMap.get(local);
        if (correlationID!=null) {
        	Quipu rv=(Quipu)_dispatcher.getRendezVousMap().get(correlationID);
        	if (rv==null)
        		_log.warn("no one waiting for: "+correlationID);
        	else {
        		_log.trace("successful correlation: "+correlationID);
        		rv.putResult(state);
        	}
        }
    }

    public void onNodeAdd(ClusterEvent event) {
        Node node=event.getNode();
        _log.info("onNodeAdd: "+getNodeName(node)+": "+node.getState());
        if (_cluster.getLocalNode()==_coordinatorNode) {
            _coordinator.queueRebalancing();
        }

        _partitionManager.update(node);
    }

    public void onNodeRemoved(ClusterEvent event) {
        Node node=event.getNode();
        _log.info("onNodeRemoved: "+getNodeName(node));
        _leavers.add(node.getDestination());
        if (_coordinator!=null)
            _coordinator.queueRebalancing();
    }


    public boolean amCoordinator() {
    	return _coordinatorNode.getDestination().equals(_cluster.getLocalNode().getDestination());
    }

    public void onNodeFailed(ClusterEvent event) {
        Node node=event.getNode();
        _log.info("NODE FAILED: "+getNodeName(node));
        if (_leavers.remove(node.getDestination())) {
            // we have already been explicitly informed of this node's wish to leave...
            _left.remove(node);
            _log.trace("onNodeFailed:"+getNodeName(node)+"- already evacuated - ignoring");
        } else {
            _log.error("onNodeFailed: "+getNodeName(node));
            if (amCoordinator()) {
                _log.error("CATASTROPHIC FAILURE on: "+getNodeName(node));
                if (_coordinator!=null)
                    _coordinator.queueRebalancing();
                else
                	_log.warn("coordinator thread not running");
            }
        }
    }

    public void onCoordinatorChanged(ClusterEvent event) {
        synchronized (_coordinatorLock) {
            _log.info("onCoordinatorChanged: "+getNodeName(event.getNode()));
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

    public Collection[] createResultSet(int numBuckets, int[] keys) {
        Collection[] c=new Collection[numBuckets];
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
        return _numPartitions;
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
            _log.info(DIndex.getNodeName(node)+" : <unknown> - {?...}");
            return 0;
        } else {
            BucketKeys keys=DIndex.getBucketKeys(node);
            int amount=keys.size();
            _log.info(DIndex.getNodeName(node)+" : "+amount+" - "+keys);
            return amount;
        }
    }

    public void onDIndexInsertionRequest(ObjectMessage om, DIndexInsertionRequest request) {
        onDIndexRequest(om, request);
    }

    public void onDIndexDeletionRequest(ObjectMessage om, DIndexDeletionRequest request) {
        onDIndexRequest(om, request);
    }

    public void onDIndexForwardRequest(ObjectMessage om, DIndexForwardRequest request) {
        onDIndexRequest(om, request);
    }

    public void onDIndexRelocationRequest(ObjectMessage om, DIndexRelocationRequest request) {
        onDIndexRequest(om, request);
    }

    protected void onDIndexRequest(ObjectMessage om, DIndexRequest request) {
        int bucketKey=request.getBucketKey(_numPartitions);
        _partitionManager.getPartition(bucketKey).dispatch(om, request);
    }

    // temporary test methods...

    public Object insert(String name) {
        try {
            ObjectMessage message=_cluster.createObjectMessage();
            message.setJMSReplyTo(_cluster.getLocalNode().getDestination());
            DIndexInsertionRequest request=new DIndexInsertionRequest(name);
            message.setObject(request);
            return _partitionManager.getPartition(getKey(name)).exchange(message, request, _inactiveTime);
        } catch (JMSException e) {
            _log.info("oops...", e);
        }
        return null;
    }

    public void remove(String name) {
        try {
            ObjectMessage message=_cluster.createObjectMessage();
            message.setJMSReplyTo(_cluster.getLocalNode().getDestination());
            DIndexDeletionRequest request=new DIndexDeletionRequest(name);
            message.setObject(request);
            _partitionManager.getPartition(getKey(name)).exchange(message, request, _inactiveTime);
        } catch (JMSException e) {
            _log.info("oops...", e);
        }
    }

    public void relocate(String name) {
        try {
            ObjectMessage message=_cluster.createObjectMessage();
            message.setJMSReplyTo(_cluster.getLocalNode().getDestination());
            DIndexRelocationRequest request=new DIndexRelocationRequest(name);
            message.setObject(request);
            _partitionManager.getPartition(getKey(name)).exchange(message, request, _inactiveTime);
        } catch (JMSException e) {
            _log.info("oops...", e);
        }
    }

    public ObjectMessage forwardAndExchange(String name, ObjectMessage message, DIndexRequest request, long timeout) {
        int key=getKey(name);
        try {
            _log.trace("wrapping request");
            request=new DIndexForwardRequest(request);
            message.setObject(request);
            return _partitionManager.getPartition(key).exchange(message, request, timeout);
        } catch (JMSException e) {
            _log.info("oops...", e);
            return null;
        }
    }

    protected int getKey(String name) {
        return Math.abs(name.hashCode()%_numPartitions);
    }

    // BucketConfig

    public String getNodeName(Destination destination) {
        Node local=_cluster.getLocalNode();
        Node node=destination.equals(local.getDestination())?local:(Node)_cluster.getNodes().get(destination);
        return getNodeName(node);
    }

    public long getInactiveTime() {
        return _inactiveTime;
    }

	public void regenerateMissingBuckets(Node[] living, Node[] leaving) {
		_partitionManager.regenerateMissingPartitions(living, leaving);
	}

	public static BucketKeys getBucketKeys(Node node) {
	    return ((BucketKeys)node.getState().get(_bucketKeysKey));
	}


    // only for use whilst developing GridState...

//    public BucketFacade[] getBuckets() {
//    	return _buckets;
//    }

}

