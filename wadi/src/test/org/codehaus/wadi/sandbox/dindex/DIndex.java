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
package org.codehaus.wadi.sandbox.dindex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.codehaus.wadi.impl.MessageDispatcher;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

public class DIndex implements ClusterListener, CoordinatorConfig {
    
    protected final static String _nodeNameKey="nodeName";
    protected final static String _bucketKeysKey="bucketKeys";
    protected final static String _timeStampKey="timeStamp";
    protected final static String _birthTimeKey="birthTime";
    
    protected final Map _distributedState;
    protected final Latch _coordinatorLatch=new Latch();
    protected final Object _coordinatorLock=new Object();
    protected final Map _bucketTransferRequestResponseRvMap=new ConcurrentHashMap();
    protected final Map _bucketTransferCommandAcknowledgementRvMap=new ConcurrentHashMap();
    protected final Map _bucketEvacuationRequestResponseRvMap=new ConcurrentHashMap();
    protected final MessageDispatcher _dispatcher;
    protected final String _nodeName;
    protected final Log _log;
    protected final int _numBuckets;
    protected final BucketFacade[] _buckets;
    protected final long _inactiveTime;
    protected final Cluster _cluster;

    public DIndex(String nodeName, int numBuckets, long inactiveTime, Cluster cluster, MessageDispatcher dispatcher, Map distributedState) {
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
            _buckets[i]=new BucketFacade(i, timeStamp, new DummyBucket(i), queueing);
    }
    
    
    protected Node _coordinatorNode;
    protected Coordinator _coordinator;
    
    public void init() throws Exception {
        _log.info("init-ing...");
        _cluster.setElectionStrategy(new SeniorityElectionStrategy());
        _cluster.addClusterListener(this);
        _distributedState.put(_nodeNameKey, _nodeName);
        _distributedState.put(_birthTimeKey, new Long(System.currentTimeMillis()));
        BucketKeys keys=new BucketKeys(_buckets);
        _distributedState.put(_bucketKeysKey, keys);
        _distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
        _log.info("local state: "+keys);
        _dispatcher.register(this, "onBucketTransferCommand", BucketTransferCommand.class);
        _dispatcher.register(this, "onBucketTransferRequest", BucketTransferRequest.class);
        _dispatcher.register(this, "onBucketEvacuationRequest", BucketEvacuationRequest.class);
        _dispatcher.register(BucketEvacuationResponse.class, _bucketEvacuationRequestResponseRvMap, _inactiveTime);
        _dispatcher.register(BucketTransferResponse.class, _bucketTransferRequestResponseRvMap, _inactiveTime);
        _dispatcher.register(BucketTransferAcknowledgement.class, _bucketTransferCommandAcknowledgementRvMap, _inactiveTime);
        
        _cluster.getLocalNode().setState(_distributedState); // this needs to be done before _cluster.start()
        _log.info("distributed state updated: "+_distributedState.get(_bucketKeysKey));
        _log.info("...init-ed");
    }

    public void start() throws Exception {
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
                facade.setContent(timeStamp, new LocalBucket(i));
                facade.dequeue();
            }
            BucketKeys k=new BucketKeys(_buckets);
            _distributedState.put(_bucketKeysKey, k);
            _distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
            _log.info("local state: "+k);
            _cluster.getLocalNode().setState(_distributedState);
            _log.info("distributed state updated: "+_distributedState.get(_bucketKeysKey));
            onCoordinatorChanged(new ClusterEvent(_cluster, _cluster.getLocalNode(), ClusterEvent.ELECTED_COORDINATOR));
            _coordinator.queueRebalancing();
        }

        _log.info("...started");
    }
    
    public void stop() throws Exception {
        _log.info("stopping...");
        
        Thread.interrupted();
        
        if (_coordinatorNode==_cluster.getLocalNode()) {
            _log.info("final Node exiting Cluster");
        } else {
            try {
                Node localNode=_cluster.getLocalNode();
                ObjectMessage om=_cluster.createObjectMessage();
                om.setJMSReplyTo(localNode.getDestination());
                om.setJMSDestination(_cluster.getDestination()); // whole cluster needs to know who is leaving - in case Coordinator fails
                om.setJMSCorrelationID(localNode.getName());
                om.setObject(new BucketEvacuationRequest());
                _dispatcher.exchange(om, _bucketEvacuationRequestResponseRvMap, _inactiveTime);
            } catch (JMSException e) {
                _log.warn("problem sending evacuation request");
            }
        }
        
        if (_coordinator!=null) {
            _coordinator.stop();
            _coordinator=null;
        }
        _log.info("...stopped");
    }
    
    public Cluster getCluster() {
        return _cluster;
    }
    
    // ClusterListener
    
    protected void updateBuckets(Node node, long timeStamp, BucketKeys keys) {
        Destination location=node.getDestination();
        int[] k=keys._keys;
        for (int i=0; i<k.length; i++) {
            int key=k[i];
            BucketFacade facade=_buckets[key];
            facade.setContentRemote(timeStamp, location);
            facade.dequeue();
        }
    }
    
    public void onNodeUpdate(ClusterEvent event) {
        Node node=event.getNode();
        _log.info("onNodeUpdate: "+getNodeName(node)+": "+node.getState());
        
        long timeStamp=((Long)node.getState().get(_timeStampKey)).longValue();
        BucketKeys keys=(BucketKeys)node.getState().get(_bucketKeysKey);
        _log.info("keys: "+keys+" - location: "+getNodeName(node));
        updateBuckets(node, timeStamp, keys);
    }
    
    public void onNodeAdd(ClusterEvent event) {
        Node node=event.getNode();
        _log.info("onNodeAdd: "+getNodeName(node));
        if (_cluster.getLocalNode()==_coordinatorNode) {
            _coordinator.queueRebalancing();
        }
        
        long timeStamp=((Long)node.getState().get(_timeStampKey)).longValue();
        BucketKeys keys=(BucketKeys)node.getState().get(_bucketKeysKey);
        _log.info("keys: "+keys+" - location: "+getNodeName(node));
        updateBuckets(node, timeStamp, keys);
    }
    
    public void onNodeRemoved(ClusterEvent event) {
        Node node=event.getNode();
        _log.info("onNodeRemoved: "+getNodeName(node));
        // NYI
        throw new UnsupportedOperationException();
    }
    
    public void onNodeFailed(ClusterEvent event) {
        Node node=event.getNode();
        _log.info("onNodeFailed: "+getNodeName(node));
        if (_leavers.remove(node.getDestination())) {
            // we have already been explicitly informed of this node's wish to leave...
            _left.remove(node);
        } else {
            // we have to assume that this was a catastrophic failure...
            _log.error("CATASTROPHIC FAILURE - NYI : "+getNodeName(node));
            // consider locking all corresponding buckets until we know what to do with them...
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
    
    /**
     * @param items - Excludables on some of which we need an exclusive lock
     * @param numItems - The number of Excludables upon which to acquire locks
     * @param timeout - the total amount of time available to acquire them
     * @return - The Excludables on which locks have been acquired
     * @throws TimeoutException - if we could not acquire the required number of locks
     */
    protected LocalBucket[] attempt(BucketFacade[] buckets, int numBuckets, long timeout) throws TimeoutException {
        // do not bother actually locking at the moment - FIXME
        
        Collection c=new ArrayList();
        for (int i=0; i<buckets.length && c.size()<numBuckets; i++) {
            Bucket bucket=buckets[i].getContent();
            if (bucket.isLocal()) // TODO - AND we acquire the lock
                c.add(bucket);
        }
        return (LocalBucket[])c.toArray(new LocalBucket[c.size()]);
        
//      Sync lock=partition.getExclusiveLock();
//      long timeout=500; // how should we work this out ? - TODO
//      if (lock.attempt(timeout))
//      acquired.add(partition);
        // use finally clause to unlock if fail to acquire...
    }
    
    /**
     * @param items - release the exclusive lock on all items in this array
     */
    protected void release(Object[] items) {
        // do not bother with locking at the moment - FIXME
        //((IndexPartition)acquired.get(i)).getExclusiveLock().release();
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
            boolean success=false;
            
            LocalBucket[] acquired=null;
            try {
                // lock partitions
                acquired=attempt(_buckets, amount, _inactiveTime);
                assert amount==acquired.length;
                long timeStamp=System.currentTimeMillis();
                
                // build request...
                _log.info("local state (before giving): "+new BucketKeys(_buckets));
                _log.info("transferring "+acquired.length+" buckets to "+getNodeName((Node)_cluster.getNodes().get(destination)));
                ObjectMessage om2=_cluster.createObjectMessage();
                om2.setJMSReplyTo(_cluster.getLocalNode().getDestination());
                om2.setJMSDestination(destination);
                om2.setJMSCorrelationID(om.getJMSCorrelationID()+"-"+destination);
                BucketTransferRequest request=new BucketTransferRequest(timeStamp, acquired);
                om2.setObject(request);
                // send it...
                ObjectMessage om3=_dispatcher.exchange(om2, _bucketTransferRequestResponseRvMap, _inactiveTime);
                // process response...
                if (om3!=null && (success=((BucketTransferResponse)om3.getObject()).getSuccess())) {
                    for (int j=0; j<acquired.length; j++) {
                        BucketFacade facade=_buckets[acquired[j].getKey()];
                        facade.setContentRemote(timeStamp, destination); // TODO - should we use a more recent ts ?
                        facade.dequeue();
                    }
                } else {
                    _log.warn("transfer unsuccessful");
                }
            } catch (Throwable t) {
                _log.warn("unexpected problem", t);
            } finally {
                release(acquired);
            }
        }
        try {
            BucketKeys keys=new BucketKeys(_buckets);
            _distributedState.put(_bucketKeysKey, keys);
            _distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
            _log.info("local state (after giving): "+keys);
            _log.info("local state updated");
            _cluster.getLocalNode().setState(_distributedState);
            _log.info("distributed state updated: "+_distributedState.get(_bucketKeysKey));
            _dispatcher.replyToMessage(om, new BucketTransferAcknowledgement(true)); // what if failure - TODO
        } catch (JMSException e) {
            _log.warn("could not acknowledge safe transfer to Coordinator", e);
        }
    }
    
    protected Node getSrcNode(ObjectMessage om) {
        try {
            return (Node)_cluster.getNodes().get(om.getJMSReplyTo());
        } catch (JMSException e) {
            _log.warn("could not read src node from message", e);
            return null;
        }
    }
    
    public void onBucketTransferRequest(ObjectMessage om, BucketTransferRequest request) {
        long timeStamp=request.getTimeStamp();
        Bucket[] buckets=request.getBuckets();
        _log.info(""+timeStamp+" received "+buckets.length+" buckets from "+getNodeName(getSrcNode(om)));
        boolean success=false;
        // read incoming data into our own local model
        _log.info("local state (before receiving): "+new BucketKeys(_buckets));
        for (int i=0; i<buckets.length; i++) {
            Bucket bucket=buckets[i];
            BucketFacade facade=_buckets[bucket.getKey()];
            facade.setContent(timeStamp, bucket);
            facade.dequeue();
        }
        success=true;
        boolean acked=false;
        try {
            BucketKeys keys=new BucketKeys(_buckets);
            _distributedState.put(_bucketKeysKey, keys);
            _distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
            _log.info("local state (after receiving): "+keys);
            _cluster.getLocalNode().setState(_distributedState);
            _log.info("distributed state updated: "+_distributedState.get(_bucketKeysKey));
        } catch (JMSException e) {
            _log.error("could not update distributed state", e);
        }
        // acknowledge safe receipt to donor
        try {
            _dispatcher.replyToMessage(om, new BucketTransferResponse(success));
            _log.info("sent TransferResponse");
            acked=true;
            
        } catch (JMSException e) {
            _log.warn("problem acknowledging reciept of IndexPartitions - donor may have died", e);
        }
        if (acked) {
            // unlock Partitions here... - TODO
        } else {
            // chuck them... - TODO
        }
    }
    
    // MyNode
    
    public void onElection(ClusterEvent event) {
        _log.info("accepting coordinatorship");
        try {
            (_coordinator=new Coordinator(this)).start();
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
        return _bucketTransferCommandAcknowledgementRvMap;
    }
    
    public void onBucketEvacuationRequest(ObjectMessage om, BucketEvacuationRequest request) {
        Node from=getSrcNode(om);
        if (from==null) {
            // very occasionally this comes through as a null - why ?
            _log.error("empty evacuation request");
            return;
        }
        
        _log.info("evacuation request from "+getNodeName(from));
        _leavers.add(from.getDestination());
        if (_coordinator!=null)
            _coordinator.queueRebalancing();
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
}
