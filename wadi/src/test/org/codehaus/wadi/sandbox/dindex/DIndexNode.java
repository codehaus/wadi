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
import org.activecluster.impl.DefaultClusterFactory;
import org.activemq.ActiveMQConnectionFactory;
import org.activemq.store.vm.VMPersistenceAdapterFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.MessageDispatcherConfig;
import org.codehaus.wadi.impl.MessageDispatcher;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

public class DIndexNode implements ClusterListener, MessageDispatcherConfig, CoordinatorConfig {

    protected final static String _nodeNameKey="nodeName";
    protected final static String _bucketKeysKey="bucketKeys";
    protected final static String _timeStampKey="timeStamp";
    protected final static String _birthTimeKey="birthTime";

    protected final long _heartbeat=5000; // unify with cluster heartbeat...

    //protected final String _clusterUri="peer://org.codehaus.wadi";
  //    protected final String _clusterUri="tcp://localhost:61616";
    protected final String _clusterUri="tcp://smilodon:61616";
    protected final ActiveMQConnectionFactory _connectionFactory=new ActiveMQConnectionFactory(_clusterUri);
    protected final DefaultClusterFactory _clusterFactory=new DefaultClusterFactory(_connectionFactory);
    protected final String _clusterName="ORG.CODEHAUS.WADI.TEST";
    protected final Map _distributedState=new ConcurrentHashMap();
    protected final Object _coordinatorSync=new Object();
    protected final Object _coordinatorLock=new Object();
    protected final Map _key2IndexPartitionNode=new ConcurrentReaderHashMap(); // contains full set of keys
    protected final Map _indexPartitionTransferRequestResponseRvMap=new ConcurrentHashMap();
    protected final Map _indexPartitionTransferCommandAcknowledgementRvMap=new ConcurrentHashMap();
    protected final Map _evacuationRvMap=new ConcurrentHashMap();
    protected final MessageDispatcher _dispatcher=new MessageDispatcher();
    protected final String _nodeName;
    protected final Log _log;
    protected final int _numBuckets;
    protected final BucketFacade[] _buckets;

    public DIndexNode(String nodeName, int numBuckets) {
        _nodeName=nodeName;
        _log=LogFactory.getLog(getClass().getName()+"#"+_nodeName);
        _numBuckets=numBuckets;
        _buckets=new BucketFacade[_numBuckets];
	long timeStamp=System.currentTimeMillis();
        for (int i=0; i<_numBuckets; i++)
	  _buckets[i]=new BucketFacade(i, timeStamp, new RemoteBucket(i, null)); // need to be somehow locked and released as soon as we know location...
        System.setProperty("activemq.persistenceAdapterFactory", VMPersistenceAdapterFactory.class.getName()); // peer protocol sees this
    }

    protected Cluster _cluster;
    protected Node _coordinatorNode;
    protected Coordinator _coordinator;

    public void start() throws Exception {
        _log.info("starting...");
        _connectionFactory.start();
        _cluster=_clusterFactory.createCluster(_clusterName+"-"+getContextPath());
        _cluster.setElectionStrategy(new SeniorityElectionStrategy());
        _cluster.addClusterListener(this);
        _distributedState.put(_nodeNameKey, _nodeName);
        _distributedState.put(_birthTimeKey, new Long(System.currentTimeMillis()));
        BucketKeys keys=new BucketKeys(_buckets);
        _distributedState.put(_bucketKeysKey, keys);
        _distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
        _log.info("local state: "+keys);
        _dispatcher.init(this);
        _dispatcher.register(this, "onIndexPartitionsTransferCommand", IndexPartitionsTransferCommand.class);
        _dispatcher.register(this, "onIndexPartitionsTransferRequest", IndexPartitionsTransferRequest.class);
        _dispatcher.register(this, "onEvacuationRequest", EvacuationRequest.class);
        _dispatcher.register(EvacuationResponse.class, _evacuationRvMap, _heartbeat);
        _dispatcher.register(IndexPartitionsTransferResponse.class, _indexPartitionTransferRequestResponseRvMap, 5000);
        _dispatcher.register(IndexPartitionsTransferAcknowledgement.class, _indexPartitionTransferCommandAcknowledgementRvMap, 5000);

        _cluster.getLocalNode().setState(_distributedState);
	_log.info("distributed state updated: "+_distributedState.get(_bucketKeysKey));
        _log.info("starting Cluster...");
        _cluster.start();
        _log.info("...Cluster started");
        _log.info("...started");

        synchronized (_coordinatorSync) {
            _coordinatorSync.wait(_clusterFactory.getInactiveTime());
	    _log.info("waking...");
        }

        synchronized (_coordinatorLock) {
            // If our wait timed out, then we must be the coordinator
            // behave accordingly...
            if (_coordinatorNode==null) {
                _log.info("we are first");
                _log.info("allocating "+_numBuckets+" buckets");
		long timeStamp=System.currentTimeMillis();
                synchronized (_buckets) {
                    for (int i=0; i<_numBuckets; i++)
		      _buckets[i].setContent(timeStamp, new LocalBucket(i));
                }
                BucketKeys k=new BucketKeys(_buckets);
                _distributedState.put(_bucketKeysKey, k);
		_distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
                _log.info("local state: "+k);
                onCoordinatorChanged(new ClusterEvent(_cluster, _cluster.getLocalNode(), ClusterEvent.ELECTED_COORDINATOR));
                _coordinator.queueRebalancing();
            } else {
                _log.info("we are not first");
            }
//            try {
//                _cluster.getLocalNode().setState(_distributedState);
//            } catch (JMSException e) {
//                _log.error("could not update distributed state");
//            }
        }
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
                om.setObject(new EvacuationRequest());
                _dispatcher.exchange(om, _evacuationRvMap, _heartbeat);
            } catch (JMSException e) {
                _log.warn("problem sending evacuation request");
            }
        }

        if (_coordinator!=null) {
            _coordinator.stop();
            _coordinator=null;
        }
        _cluster.stop();
        _connectionFactory.stop();
        _log.info("...stopped");
    }

    protected String getContextPath() {
        return "/";
    }

    public Cluster getCluster() {
        return _cluster;
    }

    // ClusterListener

  protected void updateBuckets(Node node, long timeStamp, BucketKeys keys) {
    Destination location=node.getDestination();
    int[] k=keys._keys;
    synchronized (_buckets) {
      for (int i=0; i<k.length; i++) {
	int key=k[i];
	BucketFacade facade=_buckets[key];
	facade.setContentRemote(timeStamp, location);
      }
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
            // we have already been explicitly informed of this nodes wish to leave...
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

            // if start() is still waiting on this sync, wake it up and make it
            // realise that this node is NOT the coordinator.
            synchronized (_coordinatorSync) {
                _coordinatorSync.notifyAll();
            }
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
        synchronized (_buckets) {
            for (int i=0; i<buckets.length && c.size()<numBuckets; i++) {
                Bucket bucket=buckets[i].getContent();
                if (bucket.isLocal()) // TODO - AND we acquire the lock
                    c.add(bucket);
            }
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
    public void onIndexPartitionsTransferCommand(ObjectMessage om, IndexPartitionsTransferCommand command) {
        Transfer[] transfers=command.getTransfers();
        for (int i=0; i<transfers.length; i++) {
            Transfer transfer=transfers[i];
            int amount=transfer.getAmount();
            Destination destination=transfer.getDestination();
            boolean success=false;
            long heartbeat=5000; // TODO - consider

            LocalBucket[] acquired=null;
            try {
                // lock partitions
                acquired=attempt(_buckets, amount, heartbeat);
                assert amount==acquired.length;
		long timeStamp=System.currentTimeMillis();

                // build request...
		_log.info("local state (before giving): "+new BucketKeys(_buckets));
                _log.info("transferring "+acquired.length+" buckets to "+getNodeName((Node)_cluster.getNodes().get(destination)));
                ObjectMessage om2=_cluster.createObjectMessage();
                om2.setJMSReplyTo(_cluster.getLocalNode().getDestination());
                om2.setJMSDestination(destination);
                om2.setJMSCorrelationID(om.getJMSCorrelationID()+"-"+destination);
                IndexPartitionsTransferRequest request=new IndexPartitionsTransferRequest(timeStamp, acquired);
                om2.setObject(request);
                // send it...
                ObjectMessage om3=_dispatcher.exchange(om2, _indexPartitionTransferRequestResponseRvMap, heartbeat);
                // process response...
                if (om3!=null && (success=((IndexPartitionsTransferResponse)om3.getObject()).getSuccess())) {
                    synchronized (_buckets) {
                        for (int j=0; j<acquired.length; j++)
			  _buckets[acquired[j].getKey()].setContentRemote(timeStamp, destination); // TODO - should we use a more recent ts ?
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
            _dispatcher.replyToMessage(om, new IndexPartitionsTransferAcknowledgement(true)); // what if failure - TODO
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

    public void onIndexPartitionsTransferRequest(ObjectMessage om, IndexPartitionsTransferRequest request) {
      long timeStamp=request.getTimeStamp();
        Bucket[] buckets=request.getBuckets();
        _log.info(""+timeStamp+" received "+buckets.length+" buckets from "+getNodeName(getSrcNode(om)));
        boolean success=false;
        // read incoming data into our own local model
        _log.info("local state (before receiving): "+new BucketKeys(_buckets));
        synchronized (_buckets) {
            for (int i=0; i<buckets.length; i++) {
                Bucket bucket=buckets[i];
                // we should lock these until acked - then unlock them - TODO...
                _buckets[bucket.getKey()].setContent(timeStamp, bucket);
            }
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
            _dispatcher.replyToMessage(om, new IndexPartitionsTransferResponse(success));
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

    protected static Object _exitSync=new Object();

    public static void main(String[] args) throws Exception {
        String nodeName=args[0];
        int numIndexPartitions=Integer.parseInt(args[1]);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
	      System.err.println("SHUTDOWN");
	      synchronized (_exitSync) {_exitSync.notifyAll();}
	      try {
	      synchronized (_exitSync) {_exitSync.wait();}
	      } catch (InterruptedException e) {
		// ignore
	      }
            }
        });

        DIndexNode node=new DIndexNode(nodeName, numIndexPartitions);
        node.start();

        synchronized (_exitSync) {_exitSync.wait();}

        node.stop();

        synchronized (_exitSync) {_exitSync.notifyAll();}

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
        return _indexPartitionTransferCommandAcknowledgementRvMap;
    }

    public void onEvacuationRequest(ObjectMessage om, EvacuationRequest request) {
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
            _log.info(DIndexNode.getNodeName(node)+" : <unknown> - {?...}");
            return 0;
        } else {
            BucketKeys keys=DIndexNode.getBucketKeys(node);
            int amount=keys.size();
            _log.info(DIndexNode.getNodeName(node)+" : "+amount+" - "+keys);
            return amount;
        }
    }
}
