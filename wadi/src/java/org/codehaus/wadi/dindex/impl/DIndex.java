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
import java.util.Iterator;
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
import org.codehaus.wadi.dindex.Bucket;
import org.codehaus.wadi.dindex.BucketConfig;
import org.codehaus.wadi.dindex.CoordinatorConfig;
import org.codehaus.wadi.dindex.DIndexConfig;
import org.codehaus.wadi.dindex.DIndexRequest;
import org.codehaus.wadi.impl.Dispatcher;
import org.codehaus.wadi.impl.Quipu;

import EDU.oswego.cs.dl.util.concurrent.Latch;

public class DIndex implements ClusterListener, CoordinatorConfig, BucketConfig {

    protected final static String _nodeNameKey="nodeName";
    protected final static String _bucketKeysKey="bucketKeys";
    protected final static String _timeStampKey="timeStamp";
    protected final static String _birthTimeKey="birthTime";
    protected final static String _correlationIDMapKey="correlationIDMap";

    protected final boolean _allowRegenerationOfMissingBuckets=true;
    protected final Map _distributedState;
    protected final Latch _coordinatorLatch=new Latch();
    protected final Object _coordinatorLock=new Object();
    protected final Dispatcher _dispatcher;
    protected final String _nodeName;
    protected final Log _log;
    protected final int _numBuckets;
    protected final BucketFacade[] _buckets;
    protected final long _inactiveTime;
    protected final Cluster _cluster;

    public DIndex(String nodeName, int numBuckets, long inactiveTime, Cluster cluster, Dispatcher dispatcher, Map distributedState) {
        _nodeName=nodeName;
        _log=LogFactory.getLog(getClass().getName()+"#"+_nodeName);
        _numBuckets=numBuckets;
        _cluster=cluster;
        _inactiveTime=inactiveTime;
        _dispatcher=dispatcher;
        _distributedState=distributedState;
        _buckets=new BucketFacade[_numBuckets];
        long timeStamp=System.currentTimeMillis();
        boolean queueing=true;
        for (int i=0; i<_numBuckets; i++)
            _buckets[i]=new BucketFacade(i, timeStamp, new DummyBucket(i), queueing, this);
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
        BucketKeys keys=new BucketKeys(_buckets);
        _distributedState.put(_bucketKeysKey, keys);
        _distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
        _log.info("local state: "+keys);
        _dispatcher.register(this, "onBucketTransferCommand", BucketTransferCommand.class);
        _dispatcher.register(BucketTransferAcknowledgement.class, _inactiveTime);
        _dispatcher.register(this, "onBucketTransferRequest", BucketTransferRequest.class);
        _dispatcher.register(BucketTransferResponse.class, _inactiveTime);
        _dispatcher.register(this, "onBucketEvacuationRequest", BucketEvacuationRequest.class);
        _dispatcher.register(BucketEvacuationResponse.class, _inactiveTime);
        _dispatcher.register(this, "onDIndexInsertionRequest", DIndexInsertionRequest.class);
        _dispatcher.register(DIndexInsertionResponse.class, _inactiveTime);
        _dispatcher.register(this, "onDIndexDeletionRequest", DIndexDeletionRequest.class);
        _dispatcher.register(DIndexDeletionResponse.class, _inactiveTime);
        _dispatcher.register(this, "onDIndexRelocationRequest", DIndexRelocationRequest.class);
        _dispatcher.register(DIndexRelocationResponse.class, _inactiveTime);
        _dispatcher.register(this, "onDIndexForwardRequest", DIndexForwardRequest.class);
        _dispatcher.register(this, "onBucketRepopulateRequest", BucketRepopulateRequest.class);
        _dispatcher.register(BucketRepopulateResponse.class, _inactiveTime);
        _log.info("...init-ed");
    }

    public void start() throws InterruptedException, JMSException {
        _log.info("starting...");

        _log.info("sleeping...");
        boolean isNotCoordinator=_coordinatorLatch.attempt(_inactiveTime); // wait to find out if we are the Coordinator
        _log.info("...waking");

        // If our wait timed out, then we must be the coordinator...
        if (!isNotCoordinator) {
            _log.info("allocating "+_numBuckets+" buckets");
            long timeStamp=System.currentTimeMillis();
            for (int i=0; i<_numBuckets; i++) {
                BucketFacade facade=_buckets[i];
                LocalBucket bucket=new LocalBucket(i);
                bucket.init(this);
                facade.setContent(timeStamp, bucket);
            }

            BucketKeys k=new BucketKeys(_buckets);
            _distributedState.put(_bucketKeysKey, k);
            _distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
            _log.info("local state: "+k);
            _cluster.getLocalNode().setState(_distributedState);
            _log.trace("distributed state updated: "+_cluster.getLocalNode().getState());
            onCoordinatorChanged(new ClusterEvent(_cluster, _cluster.getLocalNode(), ClusterEvent.ELECTED_COORDINATOR));
            _coordinator.queueRebalancing();
        }

        // whether we are the coordinator or not...
        for (int i=0; i<_numBuckets; i++)
            _buckets[i].dequeue();

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
        
        _dispatcher.deregister("onBucketTransferCommand", BucketTransferCommand.class, 5000);
        _dispatcher.deregister("onBucketTransferRequest", BucketTransferRequest.class, 5000);
        _dispatcher.deregister("onBucketEvacuationRequest", BucketEvacuationRequest.class, 5000);
        _dispatcher.deregister("onDIndexInsertionRequest", DIndexInsertionRequest.class, 5000);
        _dispatcher.deregister("onDIndexDeletionRequest", DIndexDeletionRequest.class, 5000);
        _dispatcher.deregister("onDIndexRelocationRequest", DIndexRelocationRequest.class, 5000);
        _dispatcher.deregister("onDIndexForwardRequest", DIndexForwardRequest.class, 5000);
        _dispatcher.deregister("onBucketRepopulateRequest", BucketRepopulateRequest.class, 5000);
        
        if (_coordinator!=null) {
        	_coordinator.stop();
        	_coordinator=null;
        }
        _log.info("...stopped");
    }

    public Cluster getCluster() {
        return _cluster;
    }

    public Dispatcher getDispatcher() {
        return _dispatcher;
    }

    // ClusterListener

    protected void updateBuckets(Node node, long timeStamp, BucketKeys keys) {
        Destination location=node.getDestination();
        int[] k=keys._keys;
        for (int i=0; i<k.length; i++) {
            int key=k[i];
            BucketFacade facade=_buckets[key];
            facade.setContentRemote(timeStamp, _dispatcher, location);
        }
    }

    public void onNodeUpdate(ClusterEvent event) {
        Node node=event.getNode();
        _log.info("onNodeUpdate: "+getNodeName(node)+": "+node.getState());

        Map state=node.getState();
        long timeStamp=((Long)state.get(_timeStampKey)).longValue();
        BucketKeys keys=(BucketKeys)state.get(_bucketKeysKey);
        updateBuckets(node, timeStamp, keys);
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

        long timeStamp=((Long)node.getState().get(_timeStampKey)).longValue();
        BucketKeys keys=(BucketKeys)node.getState().get(_bucketKeysKey);
        updateBuckets(node, timeStamp, keys);
    }

    public void onNodeRemoved(ClusterEvent event) {
        Node node=event.getNode();
        _log.info("onNodeRemoved: "+getNodeName(node));
        _leavers.add(node.getDestination());
        if (_coordinator!=null)
            _coordinator.queueRebalancing();
    }


    public void markExistingBuckets(Node[] nodes, boolean[] bucketIsPresent) {
        for (int i=0; i<nodes.length; i++) {
            Node node=nodes[i];
            if (node!=null) {
                BucketKeys keys=DIndex.getBucketKeys(node);
                if (keys!=null) {
                    int[] k=keys.getKeys();
                    for (int j=0; j<k.length; j++) {
                        int index=k[j];
                        if (bucketIsPresent[index]) {
                            _log.error("bucket "+index+" found on more than one node");
                        } else {
                            bucketIsPresent[index]=true;
                        }
                    }
                }
            }
        }
    }

    public void repopulate(Destination location, Collection[] keys) {
        assert location!=null;
        for (int i=0; i<_numBuckets; i++) {
            Collection c=keys[i];
            if (c!=null) {
                BucketFacade facade=_buckets[i];
                LocalBucket local=(LocalBucket)facade.getContent();
                for (Iterator j=c.iterator(); j.hasNext(); ) {
                    String name=(String)j.next();
                    local.put(name, location);
                }
            }
        }
    }

    public void regenerateMissingBuckets(Node[] living, Node[] leaving) {
        boolean[] bucketIsPresent=new boolean[_numBuckets];
        markExistingBuckets(living, bucketIsPresent);
        markExistingBuckets(leaving, bucketIsPresent);
        Collection missingBuckets=new ArrayList();
        for (int i=0; i<bucketIsPresent.length; i++) {
            if (!bucketIsPresent[i])
                missingBuckets.add(new Integer(i));
        }

        int numKeys=missingBuckets.size();
        if (numKeys>0) {
        	assert _allowRegenerationOfMissingBuckets;
            // convert to int[]
            int[] missingKeys=new int[numKeys];
            int key=0;
            for (Iterator i=missingBuckets.iterator(); i.hasNext(); )
                missingKeys[key++]=((Integer)i.next()).intValue();

            _log.warn("RECREATING BUCKETS...: "+missingBuckets);
            long time=System.currentTimeMillis();
            for (int i=0; i<missingKeys.length; i++) {
                int k=missingKeys[i];
                BucketFacade facade=_buckets[k];
                facade.enqueue();
                LocalBucket local=new LocalBucket(k);
                local.init(this);
                facade.setContent(time, local);
            }
            BucketKeys newKeys=new BucketKeys(_buckets);
            _log.warn("REPOPULATING BUCKETS...: "+missingBuckets);
            String correlationId=_dispatcher.nextCorrelationId();
            Quipu rv=_dispatcher.setRendezVous(correlationId, _cluster.getNodes().size());
            if (!_dispatcher.send(_cluster.getLocalNode().getDestination(), _cluster.getDestination(), correlationId, new BucketRepopulateRequest(missingKeys))) {
                _log.error("unexpected problem repopulating lost index");
            }

            // whilst we are waiting for the other nodes to get back to us, figure out which relevant sessions
            // we are carrying ourselves...
            Collection[] c=createResultSet(_numBuckets, missingKeys);
            _config.findRelevantSessionNames(_numBuckets, c);
            repopulate(_cluster.getLocalNode().getDestination(), c);

            boolean success=false;
            try {
                success=rv.waitFor(_inactiveTime);
            } catch (InterruptedException e) {
                _log.warn("unexpected interruption", e);
            }
            Collection results=rv.getResults();

            for (Iterator i=results.iterator(); i.hasNext(); ) {
                ObjectMessage message=(ObjectMessage)i.next();
                try {
                    Destination from=message.getJMSReplyTo();
                    BucketRepopulateResponse response=(BucketRepopulateResponse)message.getObject();
                    Collection[] relevantKeys=response.getKeys();

                    repopulate(from, relevantKeys);

                } catch (JMSException e) {
                    _log.warn("unexpected problem interrogating response", e);
                }
            }

            _log.warn("...BUCKETS REPOPULATED: "+missingBuckets);
            for (int i=0; i<missingKeys.length; i++) {
                int k=missingKeys[i];
                BucketFacade facade=_buckets[k];
                facade.dequeue();
            }
            // relayout dindex
            _distributedState.put(_bucketKeysKey, newKeys);
            try {
                _cluster.getLocalNode().setState(_distributedState);
		_log.trace("distributed state updated: "+_cluster.getLocalNode().getState());
            } catch (JMSException e) {
                _log.error("could not update distributed state", e);
            }
        }
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

    public void repopulateBuckets(Destination location, Collection[] keys) {
        for (int i=0; i<keys.length; i++) {
            Collection c=keys[i];
            if (c!=null) {
                for (Iterator j=c.iterator(); j.hasNext(); ) {
                    String key=(String)j.next();
                    LocalBucket bucket=(LocalBucket)_buckets[i].getContent();
                    bucket._map.put(key, location);
                }
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

    // receive a command to transfer IndexPartitions to another node
    // send them in a request, waiting for response
    // send an acknowledgement to Coordinator who sent original command
    public void onBucketTransferCommand(ObjectMessage om, BucketTransferCommand command) {
        BucketTransfer[] transfers=command.getTransfers();
        for (int i=0; i<transfers.length; i++) {
            BucketTransfer transfer=transfers[i];
            int amount=transfer.getAmount();
            Destination destination=transfer.getDestination();

            // acquire buckets for transfer...
            LocalBucket[] acquired=null;
            try {
                Collection c=new ArrayList();
                for (int j=0; j<_buckets.length && c.size()<amount; j++) {
                    BucketFacade facade=_buckets[j];
                    if (facade.isLocal()) {
                        facade.enqueue();
                        Bucket bucket=facade.getContent();
                        c.add(bucket);
                    }
                }
                acquired=(LocalBucket[])c.toArray(new LocalBucket[c.size()]);
                assert amount==acquired.length;

                long timeStamp=System.currentTimeMillis();

                // build request...
                _log.info("local state (before giving): "+new BucketKeys(_buckets));
                BucketTransferRequest request=new BucketTransferRequest(timeStamp, acquired);
                // send it...
                ObjectMessage om3=_dispatcher.exchangeSend(_cluster.getLocalNode().getDestination(), destination, request, _inactiveTime);
                // process response...
                if (om3!=null && ((BucketTransferResponse)om3.getObject()).getSuccess()) {
                    for (int j=0; j<acquired.length; j++) {
                        BucketFacade facade=null;
                        try {
                            facade=_buckets[acquired[j].getKey()];
                            facade.setContentRemote(timeStamp, _dispatcher, destination); // TODO - should we use a more recent ts ?
                        } finally {
                            if (facade!=null)
                                facade.dequeue();
                        }
                    }
                } else {
                    _log.warn("transfer unsuccessful");
                }
            } catch (Throwable t) {
                _log.warn("unexpected problem", t);
            }
        }
        try {
        	BucketKeys keys=new BucketKeys(_buckets);
        	_distributedState.put(_bucketKeysKey, keys);
        	_distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
        	_log.info("local state (after giving): "+keys);
        	String correlationID=om.getStringProperty(Dispatcher._sentFromIdKey);
        	_log.info("CORRELATIONID: "+correlationID);
        	Map correlationIDMap=(Map)_distributedState.get(_correlationIDMapKey);
        	Destination from=om.getJMSReplyTo();
        	correlationIDMap.put(from, correlationID);
        	_cluster.getLocalNode().setState(_distributedState);
        	_log.info("distributed state updated: "+_cluster.getLocalNode().getState());
        	correlateStateUpdate(_distributedState); // onStateUpdate() does not get called locally
        	correlationIDMap.remove(from);
        	// FIXME - RACE - between update of distributed state and ack - they should be one and the same thing...
        	//_dispatcher.reply(om, new BucketTransferAcknowledgement(true)); // what if failure - TODO
        } catch (JMSException e) {
        	_log.warn("could not acknowledge safe transfer to Coordinator", e);
        }
    }

    protected Node getSrcNode(ObjectMessage om) {
        try {
            Destination destination=om.getJMSReplyTo();
            Node local=_cluster.getLocalNode();
            if (destination.equals(local.getDestination()))
                return local;
            else
                return (Node)_cluster.getNodes().get(destination);
        } catch (JMSException e) {
            _log.warn("could not read src node from message", e);
            return null;
        }
    }

    public synchronized void onBucketTransferRequest(ObjectMessage om, BucketTransferRequest request) {
        long timeStamp=request.getTimeStamp();
        LocalBucket[] buckets=request.getBuckets();
        boolean success=false;
        // read incoming data into our own local model
        _log.info("local state (before receiving): "+new BucketKeys(_buckets));
        for (int i=0; i<buckets.length; i++) {
            LocalBucket bucket=buckets[i];
            bucket.init(this);
            BucketFacade facade=_buckets[bucket.getKey()];
            facade.setContent(timeStamp, bucket);
        }
        success=true;
        try {
            BucketKeys keys=new BucketKeys(_buckets);
            _distributedState.put(_bucketKeysKey, keys);
            _distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
            _log.info("local state (after receiving): "+keys);
            _cluster.getLocalNode().setState(_distributedState);
	    _log.trace("distributed state updated: "+_cluster.getLocalNode().getState());
        } catch (JMSException e) {
            _log.error("could not update distributed state", e);
        }
        // acknowledge safe receipt to donor
        if (_dispatcher.reply(om, new BucketTransferResponse(success))) {
            // unlock Partitions here... - TODO
        } else {
            _log.warn("problem acknowledging reciept of IndexPartitions - donor may have died");
            // chuck them... - TODO
        }
    }

    public Collection[] createResultSet(int numBuckets, int[] keys) {
        Collection[] c=new Collection[numBuckets];
        for (int i=0; i<keys.length; i++)
            c[keys[i]]=new ArrayList();
        return c;
    }

    public void onBucketRepopulateRequest(ObjectMessage om, BucketRepopulateRequest request) {
        int keys[]=request.getKeys();
        _log.trace("BucketRepopulateRequest ARRIVED: "+keys);
        Collection[] c=createResultSet(_numBuckets, keys);
        try {
            _log.info("findRelevantSessionNames - starting");
            _log.info(_config.getClass().getName());
            _config.findRelevantSessionNames(_numBuckets, c);
            _log.info("findRelevantSessionNames - finished");
        } catch (Throwable t) {
            _log.warn("ERROR", t);
        }
        if (!_dispatcher.reply(om, new BucketRepopulateResponse(c)))
            _log.warn("unexpected problem responding to bucket repopulation request");
    }

    // MyNode

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

    public static BucketKeys getBucketKeys(Node node) {
        return ((BucketKeys)node.getState().get(_bucketKeysKey));
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

    public int getNumItems() {
        return _numBuckets;
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

    public void onBucketEvacuationRequest(ObjectMessage om, BucketEvacuationRequest request) {
        Node from=getSrcNode(om);
        assert from!=null;
        onNodeRemoved(new ClusterEvent(_cluster, from, ClusterEvent.REMOVE_NODE));
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
        int bucketKey=request.getBucketKey(_numBuckets);
        _buckets[bucketKey].dispatch(om, request);
    }

    // temporary test methods...

    public Object insert(String name) {
        try {
            ObjectMessage message=_cluster.createObjectMessage();
            message.setJMSReplyTo(_cluster.getLocalNode().getDestination());
            DIndexInsertionRequest request=new DIndexInsertionRequest(name);
            message.setObject(request);
            return _buckets[getKey(name)].exchange(message, request, _inactiveTime);
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
            _buckets[getKey(name)].exchange(message, request, _inactiveTime);
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
            _buckets[getKey(name)].exchange(message, request, _inactiveTime);
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
            return _buckets[key].exchange(message, request, timeout);
        } catch (JMSException e) {
            _log.info("oops...", e);
            return null;
        }
    }

    protected int getKey(String name) {
        return Math.abs(name.hashCode()%_numBuckets);
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

}

