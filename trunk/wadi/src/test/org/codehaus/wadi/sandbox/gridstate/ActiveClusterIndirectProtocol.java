/**
 * 
 */
package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.activecluster.ClusterEvent;
import org.activecluster.ClusterListener;
import org.activemq.ActiveMQConnectionFactory;
import org.activemq.store.vm.VMPersistenceAdapterFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.DispatcherConfig;
import org.codehaus.wadi.ExtendedCluster;
import org.codehaus.wadi.impl.CustomClusterFactory;
import org.codehaus.wadi.impl.Dispatcher;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveBOToSO;
import org.codehaus.wadi.sandbox.gridstate.messages.MovePOToSO;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveSOToBO;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveSOToPO;
import org.codehaus.wadi.sandbox.gridstate.messages.ReadBOToPO;
import org.codehaus.wadi.sandbox.gridstate.messages.ReadPOToBO;
import org.codehaus.wadi.sandbox.gridstate.messages.WriteBOToPO;
import org.codehaus.wadi.sandbox.gridstate.messages.WritePOToBO;
import org.jgroups.Address;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public class ActiveClusterIndirectProtocol implements Protocol, BucketConfig {
	
	protected final Log _log=LogFactory.getLog(getClass());
    //protected final String _clusterUri="peer://org.codehaus.wadi";
	protected final String _clusterUri="tcp://smilodon:61616";
    protected final String _clusterName="ORG.CODEHAUS.WADI.TEST";
    protected final ActiveMQConnectionFactory _connectionFactory=new ActiveMQConnectionFactory(_clusterUri);
    protected final CustomClusterFactory _clusterFactory=new CustomClusterFactory(_connectionFactory);
	protected final Dispatcher _dispatcher;
	protected final Cluster _cluster;
	protected final long _timeout=30*1000L;
	protected final Bucket[] _buckets;
	
    class MyDispatcherConfig implements DispatcherConfig {

    	protected final Cluster _cluster;
    	
    	MyDispatcherConfig(Cluster cluster) {
    		_cluster=cluster;
    	}
    	
    	public ExtendedCluster getCluster() {
    		return (ExtendedCluster)_cluster;
    	}
    }
    
    public ActiveClusterIndirectProtocol(String nodeName, int numBuckets, BucketMapper mapper) throws Exception {
        System.setProperty("activemq.persistenceAdapterFactory", VMPersistenceAdapterFactory.class.getName());
    	_clusterFactory.setInactiveTime(100000L);
    	_cluster=_clusterFactory.createCluster(_clusterName);
    	_cluster.addClusterListener(new ClusterListener() {

			public void onNodeAdd(ClusterEvent arg0) {
			}

			public void onNodeUpdate(ClusterEvent arg0) {
			}

			public void onNodeRemoved(ClusterEvent arg0) {
			}

			public void onNodeFailed(ClusterEvent arg0) {
				_log.error("NODE FAILED");
			}

			public void onCoordinatorChanged(ClusterEvent arg0) {
			}
    	});
    	_dispatcher=new Dispatcher(nodeName);
    	_dispatcher.init(new MyDispatcherConfig(_cluster));
		_buckets=new Bucket[numBuckets];

		for (int i=0; i<numBuckets; i++) {
			Bucket bucket=new Bucket(new LocalBucket());
			bucket.init(this);
			_buckets[i]=bucket;
		}
		
		// Get - 5 messages - PO->BO->SO->PO->SO->BO
		_dispatcher.register(this, "onMessage", ReadPOToBO.class);
		_dispatcher.register(this, "onMessage", MoveBOToSO.class);
		_dispatcher.register(MoveSOToPO.class, _timeout);
		_dispatcher.register(MovePOToSO.class, _timeout);
		_dispatcher.register(MoveSOToBO.class, _timeout);
		// Get - 2 messages - PO->BO->PO (NYI)
		_dispatcher.register(ReadBOToPO.class, _timeout);

		// Put - 2 messages - PO->BO->PO
		_dispatcher.register(this, "onMessage", WritePOToBO.class);
		_dispatcher.register(WriteBOToPO.class, _timeout);

	}
	
	protected ProtocolConfig _config;
	
	public void init(ProtocolConfig config) {
		_config=config;
	}
	
	public BucketInterface createRemoteBucket() {
		return new ActiveClusterRemoteBucket(_cluster.getLocalNode().getDestination());
	}

    
    public void start() throws Exception {
    	_cluster.start();
    }
    
    public void stop() throws Exception {
    	_cluster.stop();
    }
    
    
	public Bucket[] getBuckets() {
		return _buckets;
	}

	// BucketConfig
	  
	  public Destination getLocalDestination() {
		  return _cluster.getLocalNode().getDestination();
	  }

	  public Address getLocalAddress() {
		  throw new UnsupportedOperationException("impossible");
	  }
	  
	  public Dispatcher getDispatcher() {
		  return _dispatcher;
	  }

	//--------------------------------------------------------------------------------
	// Get
	//--------------------------------------------------------------------------------

	// called on PO...
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.gridstate.Protocol#get(java.io.Serializable)
	 */
	public Serializable get(Serializable key) {
		Sync sync=null;
		try {
			sync=_config.getSOSyncs().acquire(key);
			Serializable value=null;
			Map map=_config.getMap();
			synchronized (map) {
				value=(Serializable)map.get(key);
			}
			if (value!=null)
				return value;
			else {
				// exchangeSendLoop GetPOToBO to BO
				Destination po=_cluster.getLocalNode().getDestination();
				Destination bo=_buckets[_config.getBucketMapper().map(key)].getDestination();
				ReadPOToBO request=new ReadPOToBO(key, po);
				ObjectMessage message=_dispatcher.exchangeSendLoop(po, bo, request, _timeout, 10);
				Serializable response=null;
				try {
					response=message.getObject();
				} catch (JMSException e) {
					_log.error("unexpected problem", e); // should be in loop - TODO
				}
				
				if (response instanceof ReadBOToPO) {
					// association not present
					value=null;
				} else if (response instanceof MoveSOToPO) {
					// association exists
					// associate returned value with key
					value=((MoveSOToPO)response).getValue();
					//_log.info("received "+key+"="+value+" <- SO");
					synchronized (_config.getMap()) {
						map.put(key, value);
					}
					// reply GetPOToSO to SO
					_dispatcher.reply(message, new MovePOToSO());
				}
				
				return value;
			}
		} finally {
			_log.trace("[PO] releasing sync for: "+key+" - "+sync);
			sync.release();
		}
	}
	
	// called on BO...
	public void onMessage(ObjectMessage message1, ReadPOToBO get) {
		// what if we are NOT the BO anymore ?
		// get write lock on location
		Serializable key=get.getKey();
		Sync sync=null;
		try {
			sync=_config.getBOSyncs().acquire(key);
			Bucket bucket=_buckets[_config.getBucketMapper().map(key)];
			ActiveClusterLocation location=(ActiveClusterLocation)bucket.getLocation(key);
			if (location==null) {
				_dispatcher.reply(message1,new ReadBOToPO());
				return;
			}
			// exchangeSendLoop GetBOToSO to SO
			Destination po=get.getPO();
			Destination bo=_cluster.getLocalNode().getDestination();
			Destination so=location.getDestination();
			String poCorrelationId=null;
			try {
				poCorrelationId=_dispatcher.getOutgoingCorrelationId(message1);
				//_log.info("Process Owner Correlation ID: "+poCorrelationId);
			} catch (JMSException e) {
				_log.error("unexpected problem", e);
			}
			MoveBOToSO request=new MoveBOToSO(key, po, bo, poCorrelationId);
			ObjectMessage message2=_dispatcher.exchangeSendLoop(bo, so, request, _timeout, 10);
			MoveSOToBO response=null;
			try {
				response=(MoveSOToBO)message2.getObject();
			} catch (JMSException e) {
				_log.error("unexpected problem", e); // should be sorted in loop
			}
			// alter location
			location.setDestination(get.getPO());
			
		} finally {
			_log.trace("[BO] releasing sync for: "+key+" - "+sync);
			sync.release();
		}
	}
	
	// called on SO...
	public void onMessage(ObjectMessage message1, MoveBOToSO get) {
		Serializable key=get.getKey();
		Sync sync=null;
		try {
			sync=_config.getSOSyncs().acquire(key);
			// send GetSOToPO to PO
			Destination so=_cluster.getLocalNode().getDestination();
			Destination po=get.getPO();
			Serializable value;
			Map map=_config.getMap();
			synchronized (map) {
				value=(Serializable)map.get(key);
			}
			//_log.info("sending "+key+"="+value+" -> PO");
			MoveSOToPO request=new MoveSOToPO(value);
			ObjectMessage message2=(ObjectMessage)_dispatcher.exchangeSend(so, po, request, _timeout, get.getPOCorrelationId());
			// wait
			// receive GetPOToSO
			MovePOToSO response=null;
			try {
				response=(MovePOToSO)message2.getObject();
				// remove association
				synchronized (map) {
					map.remove(key);
				}
				// send GetSOToBO to BO
				Destination bo=get.getBO();
				_dispatcher.reply(message1,new MoveSOToBO());
			} catch (JMSException e) {
				_log.error("unexpected problem", e);
			}
		} finally {
			_log.trace("[SO] releasing sync for: "+key+" - "+sync);
			sync.release();
		}
	}
	
	
	//--------------------------------------------------------------------------------
	// Put
	//--------------------------------------------------------------------------------

	// called on PO...
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.gridstate.Protocol#put(java.io.Serializable, java.io.Serializable, boolean, boolean)
	 */
	public Serializable put(Serializable key, Serializable value, boolean overwrite, boolean returnOldValue) {
		boolean removal=(value==null);
		Map map=_config.getMap();
		Sync sync=null;
		try {
			sync=_config.getSOSyncs().acquire(key);
			
			if (!removal) { // removals must do the round trip to BO
				boolean local;
				synchronized (map) {
					local=map.containsKey(key);
				}
				
				if (local) {
					// local
					if (overwrite) {
						synchronized (map) {
							Serializable oldValue=(Serializable)map.put(key, value);
							return returnOldValue?oldValue:null;
						}
					} else {
						return Boolean.FALSE;
					}
				}
			}
			
			// absent or remote
			// exchangeSendLoop PutPOToBO to BO
			Destination po=_cluster.getLocalNode().getDestination();
			Destination bo=_buckets[_config.getBucketMapper().map(key)].getDestination();
			WritePOToBO request=new WritePOToBO(key, value==null, overwrite, returnOldValue, po);
			ObjectMessage message=_dispatcher.exchangeSendLoop(po, bo, request, _timeout, 10);
			Serializable response=null;
			try {
				response=message.getObject();
			} catch (JMSException e) {
				_log.error("unexpected problem", e); // should be in loop - TODO
			}
			
			// 2 possibilities - 
			// PutBO2PO - Absent
			if (response instanceof WriteBOToPO) {
				if (overwrite) {
					synchronized (map) {
						Serializable oldValue=(Serializable)(removal?map.remove(key):map.put(key, value));
						return returnOldValue?oldValue:null;
					}
				} else {
					if (((WriteBOToPO)response).getSuccess()) {
						synchronized (map) {
							map.put(key, value);
						}
						return Boolean.TRUE;
					} else {
						return Boolean.FALSE;
					}
				}
			} else if (response instanceof MoveSOToPO) {
				// Present - remote
				// reply GetPOToSO to SO
				_dispatcher.reply(message, new MovePOToSO());
				synchronized (map) {
					if (removal)
						map.remove(key);
					else
						map.put(key, value);
				}
				return ((MoveSOToPO)response).getValue();
			} else {
				_log.error("unexpected response: "+response.getClass().getName());
				return null;
			}
			
		} finally {
			_log.trace("[PO] releasing sync for: "+key+" - "+sync);
			sync.release();
		}
	}

	// called on BO...
	public void onMessage(ObjectMessage message1, WritePOToBO write) {
		// what if we are NOT the BO anymore ?
		Serializable key=write.getKey();
		Bucket bucket=_buckets[_config.getBucketMapper().map(key)];
		Map bucketMap=bucket.getMap();
		Sync sync=null;
		try {
			sync=_config.getBOSyncs().acquire(key);
			Location location=write.getValueIsNull()?null:new ActiveClusterLocation(write.getPO());
			// remove or update location, remembering old value
			ActiveClusterLocation oldLocation=(ActiveClusterLocation)(location==null?bucketMap.remove(key):bucketMap.put(key, location));
			// if we are not allowed to overwrite, and we have...
			if (!write.getOverwrite() && oldLocation!=null) {
				//  undo our change
				bucketMap.put(key, oldLocation);
				// send BOToPO - failure
				_dispatcher.reply(message1, new WriteBOToPO(false));
			} else if (oldLocation==null || (write.getPO().equals(oldLocation.getDestination()))) {
				// if there was previously no SO, or there was, but it was PO ...
				// then there is no need to go and remove the old value from the old SO
				// send BOToPO - success
				_dispatcher.reply(message1, new WriteBOToPO(true));
			} else {
				// previous value needs removing and possibly returning...
				// send BOToSO...

				String poCorrelationId=null;
				try {
					poCorrelationId=_dispatcher.getOutgoingCorrelationId(message1);
					//_log.info("Process Owner Correlation ID: "+poCorrelationId);
				} catch (JMSException e) {
					_log.error("unexpected problem", e);
				}
				Destination po=write.getPO();
				Destination bo=_cluster.getLocalNode().getDestination();
				Destination so=oldLocation.getDestination();
				MoveBOToSO request=new MoveBOToSO(key, po, bo, poCorrelationId);
				ObjectMessage message2=_dispatcher.exchangeSendLoop(bo, so, request, _timeout, 10);
				MoveSOToBO response=null;
				try {
					response=(MoveSOToBO)message2.getObject();
				} catch (JMSException e) {
					_log.error("unexpected problem", e); // should be sorted in loop
				}
			}
			
		} finally {
			_log.trace("[BO] releasing sync for: "+key+" - "+sync);
			sync.release();
		}
	}
	
	//--------------------------------------------------------------------------------
	// Remove
	//--------------------------------------------------------------------------------

	// called on PO...
	public Serializable remove(Serializable key, boolean returnOldValue) {
		return put(key, null, true, returnOldValue); // a remove is a put(key, null)...
	}

}