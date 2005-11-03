package org.codehaus.wadi.dindex.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.activecluster.ClusterEvent;
import org.activecluster.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.dindex.Bucket;
import org.codehaus.wadi.dindex.BucketConfig;
import org.codehaus.wadi.dindex.DIndexConfig;
import org.codehaus.wadi.dindex.PartitionManager;
import org.codehaus.wadi.impl.Dispatcher;
import org.codehaus.wadi.impl.Quipu;

/**
 * A Simple PartitionManager.
 * 
 * @author jules
 *
 */
public class SimplePartitionManager implements PartitionManager {

	interface Callback {void onNodeRemoved(ClusterEvent event);}
	
	protected final static String _bucketKeysKey="bucketKeys";
    protected final static String _timeStampKey="timeStamp";
    protected final static String _correlationIDMapKey="correlationIDMap";

    protected final String _nodeName;
    protected final Log _log;
    protected final int _numPartitions;
    protected final BucketFacade[] _partitions;
    protected final Cluster _cluster;
    protected final BucketConfig _partitionConfig;
    protected final Dispatcher _dispatcher;
    protected final Map _distributedState;
    protected final long _inactiveTime;
	protected final boolean _allowRegenerationOfMissingPartitions = true;
	protected final Callback _callback;
    
    public SimplePartitionManager(String nodeName, int numPartitions, BucketConfig config, Cluster cluster, Dispatcher dispatcher, Map distributedState, long inactiveTime, Callback callback) {
    	_nodeName=nodeName;
    	_log=LogFactory.getLog(getClass().getName()+"#"+_nodeName);
    	_numPartitions=numPartitions;

        _partitions=new BucketFacade[_numPartitions];
        long timeStamp=System.currentTimeMillis();
        boolean queueing=true;
        for (int i=0; i<_numPartitions; i++)
            _partitions[i]=new BucketFacade(i, timeStamp, new DummyBucket(i), queueing, config);
        
        _partitionConfig=config;
        _cluster=cluster;
        _dispatcher=dispatcher;
        _distributedState=distributedState;
        _inactiveTime=inactiveTime;
        _callback=callback;
    }
    
    protected DIndexConfig _dindexConfig;
    
    public void init(DIndexConfig config) {
    	_dindexConfig=config;
    	_log.trace("init");
    	// attach relevant message handlers to dispatcher...
        _dispatcher.register(this, "onBucketTransferCommand", BucketTransferCommand.class);
        _dispatcher.register(BucketTransferAcknowledgement.class, _inactiveTime);
        _dispatcher.register(this, "onBucketTransferRequest", BucketTransferRequest.class);
        _dispatcher.register(BucketTransferResponse.class, _inactiveTime);
        _dispatcher.register(this, "onBucketEvacuationRequest", BucketEvacuationRequest.class);
        _dispatcher.register(BucketEvacuationResponse.class, _inactiveTime);
        _dispatcher.register(this, "onBucketRepopulateRequest", BucketRepopulateRequest.class);
        _dispatcher.register(BucketRepopulateResponse.class, _inactiveTime);
    }

    public void start() throws Exception {
    	_log.trace("start");
    }
    
    public void stop() throws Exception {
    	_log.trace("stop");
    	// detach relevant message handlers from dispatcher...
        _dispatcher.deregister("onBucketTransferCommand", BucketTransferCommand.class, 5000);
        _dispatcher.deregister("onBucketTransferRequest", BucketTransferRequest.class, 5000);
        _dispatcher.deregister("onBucketEvacuationRequest", BucketEvacuationRequest.class, 5000);
        _dispatcher.deregister("onBucketRepopulateRequest", BucketRepopulateRequest.class, 5000);
    }
    
	public BucketFacade getPartition(int bucket) {
		return _partitions[bucket];
	}
	
	// a node wants to shutdown...
	public void onBucketEvacuationRequest(ObjectMessage om, BucketEvacuationRequest request) {
	    Node from=getSrcNode(om);
	    assert from!=null;
	    _callback.onNodeRemoved(new ClusterEvent(_cluster, from, ClusterEvent.REMOVE_NODE));
	}

	// a node wants to rebuild a lost bucket
	public void onBucketRepopulateRequest(ObjectMessage om, BucketRepopulateRequest request) {
	    int keys[]=request.getKeys();
	    _log.trace("BucketRepopulateRequest ARRIVED: "+keys);
	    Collection[] c=createResultSet(_numPartitions, keys);
	    try {
	        _log.info("findRelevantSessionNames - starting");
	        _log.info(_partitionConfig.getClass().getName());
	        _dindexConfig.findRelevantSessionNames(_numPartitions, c);
	        _log.info("findRelevantSessionNames - finished");
	    } catch (Throwable t) {
	        _log.warn("ERROR", t);
	    }
	    if (!_dispatcher.reply(om, new BucketRepopulateResponse(c)))
	        _log.warn("unexpected problem responding to bucket repopulation request");
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
	            for (int j=0; j<_numPartitions && c.size()<amount; j++) {
	            	BucketFacade facade=_partitions[j];
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
	            _log.info("local state (before giving): "+getPartitionKeys());
	            BucketTransferRequest request=new BucketTransferRequest(timeStamp, acquired);
	            // send it...
	            ObjectMessage om3=_dispatcher.exchangeSend(_cluster.getLocalNode().getDestination(), destination, request, _inactiveTime);
	            // process response...
	            if (om3!=null && ((BucketTransferResponse)om3.getObject()).getSuccess()) {
	                for (int j=0; j<acquired.length; j++) {
	                    BucketFacade facade=null;
	                    try {
	                    	facade=_partitions[acquired[j].getKey()];
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
	    	BucketKeys keys=getPartitionKeys();
	    	_distributedState.put(_bucketKeysKey, keys);
	    	_distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
	    	_log.info("local state (after giving): "+keys);
	    	String correlationID=Dispatcher.getOutgoingCorrelationId(om);
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

	// receive a transfer of partitions
	public synchronized void onBucketTransferRequest(ObjectMessage om, BucketTransferRequest request) {
	    long timeStamp=request.getTimeStamp();
	    LocalBucket[] buckets=request.getBuckets();
	    boolean success=false;
	    // read incoming data into our own local model
	    _log.info("local state (before receiving): "+getPartitionKeys());
	    for (int i=0; i<buckets.length; i++) {
	        LocalBucket bucket=buckets[i];
	        bucket.init(_partitionConfig);
	        BucketFacade facade=getPartition(bucket.getKey());
	        facade.setContent(timeStamp, bucket);
	    }
	    success=true;
	    try {
	        BucketKeys keys=getPartitionKeys();
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

	// TODO - duplicate code (from DIndex)
    public Collection[] createResultSet(int numBuckets, int[] keys) {
        Collection[] c=new Collection[numBuckets];
        for (int i=0; i<keys.length; i++)
            c[keys[i]]=new ArrayList();
        return c;
    }

	// ClusterListener
	
	public void update(Node node) {
        Map state=node.getState();
        long timeStamp=((Long)state.get(_timeStampKey)).longValue();
        BucketKeys keys=(BucketKeys)state.get(_bucketKeysKey);
	    Destination location=node.getDestination();
	    int[] k=keys._keys;
	    for (int i=0; i<k.length; i++) {
	        int key=k[i];
	        BucketFacade facade=_partitions[key];
	        facade.setContentRemote(timeStamp, _dispatcher, location);
	    }
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

	public void regenerateMissingPartitions(Node[] living, Node[] leaving) {
	    boolean[] bucketIsPresent=new boolean[_numPartitions];
	    markExistingBuckets(living, bucketIsPresent);
	    markExistingBuckets(leaving, bucketIsPresent);
	    Collection missingBuckets=new ArrayList();
	    for (int i=0; i<bucketIsPresent.length; i++) {
	        if (!bucketIsPresent[i])
	            missingBuckets.add(new Integer(i));
	    }
	
	    int numKeys=missingBuckets.size();
	    if (numKeys>0) {
	    	assert _allowRegenerationOfMissingPartitions;
	        // convert to int[]
	        int[] missingKeys=new int[numKeys];
	        int key=0;
	        for (Iterator i=missingBuckets.iterator(); i.hasNext(); )
	            missingKeys[key++]=((Integer)i.next()).intValue();
	
	        _log.warn("RECREATING BUCKETS...: "+missingBuckets);
	        long time=System.currentTimeMillis();
	        for (int i=0; i<missingKeys.length; i++) {
	            int k=missingKeys[i];
	            BucketFacade facade=_partitions[k];
	            facade.enqueue();
	            LocalBucket local=new LocalBucket(k);
	            local.init(_partitionConfig);
	            facade.setContent(time, local);
	        }
	        BucketKeys newKeys=getPartitionKeys();
	        _log.warn("REPOPULATING BUCKETS...: "+missingBuckets);
	        String correlationId=_dispatcher.nextCorrelationId();
	        Quipu rv=_dispatcher.setRendezVous(correlationId, _cluster.getNodes().size());
	        if (!_dispatcher.send(_cluster.getLocalNode().getDestination(), _cluster.getDestination(), correlationId, new BucketRepopulateRequest(missingKeys))) {
	            _log.error("unexpected problem repopulating lost index");
	        }
	
	        // whilst we are waiting for the other nodes to get back to us, figure out which relevant sessions
	        // we are carrying ourselves...
	        Collection[] c=createResultSet(_numPartitions, missingKeys);
	        _dindexConfig.findRelevantSessionNames(_numPartitions, c);
	        repopulate(_cluster.getLocalNode().getDestination(), c);
	
	        //boolean success=false;
	        try {
	            /*success=*/rv.waitFor(_inactiveTime);
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
	            BucketFacade facade=getPartition(k);
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

	public BucketKeys getPartitionKeys() {
		 return new BucketKeys(_partitions);
	}

	public void repopulate(Destination location, Collection[] keys) {
	    assert location!=null;
	    for (int i=0; i<_numPartitions; i++) {
	        Collection c=keys[i];
	        if (c!=null) {
	            BucketFacade facade=_partitions[i];
	            LocalBucket local=(LocalBucket)facade.getContent();
	            for (Iterator j=c.iterator(); j.hasNext(); ) {
	                String name=(String)j.next();
	                local.put(name, location);
	            }
	        }
	    }
	}

	public void localise() {
	    _log.info("allocating "+_numPartitions+" buckets");
	    long timeStamp=System.currentTimeMillis();
	    for (int i=0; i<_numPartitions; i++) {
	        BucketFacade facade=_partitions[i];
	        LocalBucket bucket=new LocalBucket(i);
	        bucket.init(_partitionConfig);
	        facade.setContent(timeStamp, bucket);
	    }
	}

	// TODO - duplicate code - see DIndex...
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

	public void dequeue() {
	    for (int i=0; i<_numPartitions; i++)
	        _partitions[i].dequeue();
	}

//	public void repopulateBuckets(Destination location, Collection[] keys) {
//	    for (int i=0; i<keys.length; i++) {
//	        Collection c=keys[i];
//	        if (c!=null) {
//	            for (Iterator j=c.iterator(); j.hasNext(); ) {
//	                String key=(String)j.next();
//	                LocalBucket bucket=(LocalBucket)_partitions[i].getContent();
//	                bucket._map.put(key, location);
//	            }
//	        }
//	    }
//	}
    
}
