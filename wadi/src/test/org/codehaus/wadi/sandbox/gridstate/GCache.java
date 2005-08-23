package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheEntry;
import javax.cache.CacheException;
import javax.cache.CacheListener;
import javax.cache.CacheStatistics;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.activecluster.LocalNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.Dispatcher;
import org.codehaus.wadi.impl.Utils;
import org.codehaus.wadi.sandbox.gridstate.messages.ReadBOToPO;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveBOToSO;
import org.codehaus.wadi.sandbox.gridstate.messages.ReadPOToBO;
import org.codehaus.wadi.sandbox.gridstate.messages.MovePOToSO;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveSOToBO;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveSOToPO;
import org.codehaus.wadi.sandbox.gridstate.messages.WriteBOToPO;
import org.codehaus.wadi.sandbox.gridstate.messages.WritePOToBO;

import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * Geronimo is going to need a standard API for lookup of sessions across the Cluster.
 * JCache is the obvious choice.
 * This will allow the plugging of either e.g. GCache (WADI), Tangosol's Coherence or IBMs solution without changing of Geronimo code.
 * In fact, this will allow WADI to sit on top of any of these three.
 * 
 * GCache is a JCache compatible interface onto DIndex - WADI's own distributed index, which fulfills
 * WADI's requirements for this lookup...
 * 
 * @author jules
 *
 */
public class GCache implements Cache, BucketConfig {

	protected final Log _log=LogFactory.getLog(getClass().getName());

	protected final Protocol _protocol;
	protected final Bucket[] _buckets;
	protected final Dispatcher _dispatcher;
	protected final BucketMapper _mapper;
	protected final Cluster _cluster;
	protected final Map _map=new HashMap();
	
	protected final SyncMap _boSyncs=new SyncMap("BO");
	protected final SyncMap _soSyncs=new SyncMap("PO/SO");
	protected final SyncMap _poSyncs=_soSyncs;
	protected final long _timeout=60*60*1000L;
	  
	
	public GCache(String nodeName, int numBuckets, Dispatcher dispatcher, BucketMapper mapper) {
		_buckets=new Bucket[numBuckets];
		_dispatcher=dispatcher;
		_cluster=_dispatcher.getCluster();
		_protocol=new IndirectProtocol();
		_mapper=mapper;
		
		for (int i=0; i<numBuckets; i++) {
			Bucket bucket=new Bucket(new LocalBucket());
			bucket.init(this);
			_buckets[i]=bucket;
		}
	}
	
	public class IndirectProtocol implements Protocol {
		
		public IndirectProtocol() {
			
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
				sync=_poSyncs.acquire(key);
				Serializable value=null;
				synchronized (_map) {
					value=(Serializable)_map.get(key);
				}
				if (value!=null)
					return value;
				else {
					// exchangeSendLoop GetPOToBO to BO
					Destination po=_cluster.getLocalNode().getDestination();
					Destination bo=_buckets[_mapper.map(key)].getDestination();
					ReadPOToBO request=new ReadPOToBO(key, po);
					ObjectMessage message=_dispatcher.exchangeSendLoop(po, bo, request, _timeout);
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
						synchronized (_map) {
							_map.put(key, value);
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
				sync=_boSyncs.acquire(key);
				Bucket bucket=_buckets[_mapper.map(key)];
				Location location=bucket.getLocation(key);
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
				ObjectMessage message2=_dispatcher.exchangeSendLoop(bo, so, request, _timeout);
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
				sync=_soSyncs.acquire(key);
				// send GetSOToPO to PO
				Destination so=_cluster.getLocalNode().getDestination();
				Destination po=get.getPO();
				Serializable value;
				synchronized (_map) {
					value=(Serializable)_map.get(key);
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
					synchronized (_map) {
						_map.remove(key);
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
			Sync sync=null;
			try {
				sync=_poSyncs.acquire(key);
				
				if (!removal) { // removals must do the round trip to BO
					boolean local;
					synchronized (_map) {
						local=_map.containsKey(key);
					}
					
					if (local) {
						// local
						if (overwrite) {
							synchronized (_map) {
								Serializable oldValue=(Serializable)_map.put(key, value);
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
				Destination bo=_buckets[_mapper.map(key)].getDestination();
				WritePOToBO request=new WritePOToBO(key, value==null, overwrite, returnOldValue, po);
				ObjectMessage message=_dispatcher.exchangeSendLoop(po, bo, request, _timeout);
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
						synchronized (_map) {
							Serializable oldValue=(Serializable)(removal?_map.remove(key):_map.put(key, value));
							return returnOldValue?oldValue:null;
						}
					} else {
						if (((WriteBOToPO)response).getSuccess()) {
							synchronized (_map) {
								_map.put(key, value);
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
					synchronized (_map) {
						if (removal)
							_map.remove(key);
						else
							_map.put(key, value);
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
			Bucket bucket=_buckets[_mapper.map(key)];
			Map bucketMap=bucket.getMap();
			Sync sync=null;
			try {
				sync=_boSyncs.acquire(key);
				Location location=write.getValueIsNull()?null:new Location(write.getPO());
				// remove or update location, remembering old value
				Location oldLocation=(Location)(location==null?bucketMap.remove(key):bucketMap.put(key, location));
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
					ObjectMessage message2=_dispatcher.exchangeSendLoop(bo, so, request, _timeout);
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
	
  /*
   * second pass
   */
	public boolean containsKey(Object key) {
		Sync sync=null;
		try {
			sync=_soSyncs.acquire((Serializable)key);
			synchronized (_map) {
				return _map.containsKey(key);
			}
		} finally {
			sync.release();
		}
	}

  /*
   * third pass
   */
  public boolean containsValue(Object value) {
	  throw new UnsupportedOperationException();
  }

  /*
   * third pass
   */
  public Set entrySet() {
	  throw new UnsupportedOperationException();
  }

  /*
   * second pass
   */
  public boolean isEmpty() {
	  throw new UnsupportedOperationException();
  }

  /*
   * second pass
   */
  public Set keySet() {
	  throw new UnsupportedOperationException();
  }

  /*
   * first pass
   */
  public void putAll(Map t) {
    // TODO Auto-generated method stub
  }

  /*
   * first pass
   */
  public int size() {
	  return getCacheStatistics().getObjectCount();
  }

  /*
   * third pass
   */
  public Collection values() {
	  throw new UnsupportedOperationException();
  }

  /*
   * first pass
   */
  public Object get(Object key) {
	  return _protocol.get((Serializable)key);
  }

  /*
   * first/second pass
   */
  public Map getAll(Collection keys) throws CacheException {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * second pass ?
   */
  public void load(Object key) throws CacheException {
	  throw new UnsupportedOperationException();
  }

  /*
   * second pass ?
   */
  public void loadAll(Collection keys) throws CacheException {
	  throw new UnsupportedOperationException();
  }

  /*
   * first pass ?
   */
  public Object peek(Object key) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * first pass ?
   */
  public Object put(Object key, Object value) {
	  return put(key, value, true, true);
  }
  
  // for WADI
  public boolean putFirst(Object key, Object value) {
	  return ((Boolean)put(key, value, false, true)).booleanValue();
  }
  
  // for WADI
  protected Serializable put(Object key, Object value, boolean overwrite, boolean returnOldValue) {
	  return _protocol.put((Serializable)key, (Serializable)value, overwrite, returnOldValue);
  }

  /*
   * first pass ?
   * interesting - perhaps this is how we make location accessible
   */
  public CacheEntry getCacheEntry(Object key) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * not sure ?
   */
  public CacheStatistics getCacheStatistics() {
    // TODO Auto-generated method stub
    return null;
    
    // needs to return :
    // objectCount
    // hits
    // misses
    
    // we will do best effort on all of these
    // they can be included in each node's distributed state and aggregated on demand
  }

  /*
   * first pass
   */
  public Object remove(Object key) {
	  return _protocol.remove((Serializable)key, true);
  }

  public Object remove(Object key, boolean returnOldValue) {
	  return _protocol.remove((Serializable)key, returnOldValue);
  }
  
  /*
   * third pass
   */
  public void clear() {
	  throw new UnsupportedOperationException();
  }

  /*
   * first pass ?
   */
  public void evict() {
    // TODO Auto-generated method stub
  }

  /*
   * second/third pass
   */
  public void addListener(CacheListener listener) {
	  throw new UnsupportedOperationException();
  }

  /*
   * second/third pass
   */
  public void removeListener(CacheListener listener) {
	  throw new UnsupportedOperationException();
  }
  
  // BucketConfig
  
  public LocalNode getLocalNode() {
	  return _cluster.getLocalNode();
  }
  
  public Dispatcher getDispatcher() {
	  return _dispatcher;
  }

  // Proprietary
  
  public void start() {}
  public void stop() {}
  
  public Cluster getCluster() {
	  return _cluster;
  }
  
  public Bucket[] getBuckets() {
	  return _buckets;
  }
  
  // for testing...
  public Map getMap() {
	  return _map;
  }
  
}
