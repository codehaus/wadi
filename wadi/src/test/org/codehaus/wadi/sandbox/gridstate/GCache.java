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
import org.codehaus.wadi.sandbox.gridstate.messages.GetBOToPO;
import org.codehaus.wadi.sandbox.gridstate.messages.GetBOToSO;
import org.codehaus.wadi.sandbox.gridstate.messages.GetPOToBO;
import org.codehaus.wadi.sandbox.gridstate.messages.GetPOToSO;
import org.codehaus.wadi.sandbox.gridstate.messages.GetSOToBO;
import org.codehaus.wadi.sandbox.gridstate.messages.GetSOToPO;
import org.codehaus.wadi.sandbox.gridstate.messages.PutBOToPO;
import org.codehaus.wadi.sandbox.gridstate.messages.PutPOToBO;
import org.codehaus.wadi.sandbox.gridstate.messages.PutSOToPO;

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
			_dispatcher.register(this, "onMessage", GetPOToBO.class);
			_dispatcher.register(this, "onMessage", GetBOToSO.class);
			_dispatcher.register(GetSOToPO.class, 2000);
			_dispatcher.register(GetPOToSO.class, 2000);
			_dispatcher.register(GetSOToBO.class, 2000);
			// Get - 2 messages - PO->BO->PO (NYI)
			_dispatcher.register(GetBOToPO.class, 2000);

			// Put - 2 messages - PO->BO->PO
			_dispatcher.register(this, "onMessage", PutPOToBO.class);
			_dispatcher.register(PutBOToPO.class, 2000);
			
		}

		//--------------------------------------------------------------------------------
		// Get
		//--------------------------------------------------------------------------------

		// called on PO...
		/* (non-Javadoc)
		 * @see org.codehaus.wadi.sandbox.gridstate.Protocol#get(java.io.Serializable)
		 */
		public Serializable get(Serializable key) {
			// lock map [partition]
			synchronized (_map) { // naive - should only lock map partition...
				Serializable value=(Serializable)_map.get(key);
				if (value!=null)
					return value;
				else {
					// exchangeSendLoop GetPOToBO to BO
					Destination po=_cluster.getLocalNode().getDestination();
					Destination bo=_buckets[_mapper.map(key)].getDestination();
					GetPOToBO request=new GetPOToBO(key, po);
					ObjectMessage message=_dispatcher.exchangeSendLoop(po, bo, request, 2000L);
					Serializable response=null;
					try {
						response=message.getObject();
					} catch (JMSException e) {
						_log.error("unexpected problem", e); // should be in loop - TODO
					}
					
					if (response instanceof GetBOToPO) {
						// association not present
						value=null;
					} else if (response instanceof GetSOToPO) {
						// association exists
						// associate returned value with key
						value=((GetSOToPO)response).getValue();
						_map.put(key, value);
						
						// reply GetPOToSO to SO
						_dispatcher.reply(message, new GetPOToSO());
					}
					
					return value;
					// release map [partition]
				}
			}
		}
		
		// called on BO...
		public void onMessage(ObjectMessage message1, GetPOToBO get) {
			// what if we are NOT the BO anymore ?
			Serializable key=get.getKey();
			Bucket bucket=_buckets[_mapper.map(key)];
			Location location=bucket.getLocation(key);
			if (location==null) {
				_dispatcher.reply(message1,new GetBOToPO());
				return;
			}
			Sync locationLock=location.getLock().writeLock();
			try {
				// get write lock on location
				Utils.safeAcquire(locationLock); // this should be done inside bucket lock
				// exchangeSendLoop GetBOToSO to SO
				Destination po=get.getPO();
				Destination bo=_cluster.getLocalNode().getDestination();
				Destination so=location.getDestination();
				String poCorrelationId=null;
				try {
					poCorrelationId=_dispatcher.getOutgoingCorrelationId(message1);
					_log.info("Process Owner Correlation ID: "+poCorrelationId);
				} catch (JMSException e) {
					_log.error("unexpected problem", e);
				}
				GetBOToSO request=new GetBOToSO(key, po, bo, poCorrelationId);
				ObjectMessage message2=_dispatcher.exchangeSendLoop(bo, so, request, 2000L);
				GetSOToBO response=null;
				try {
					response=(GetSOToBO)message2.getObject();
				} catch (JMSException e) {
					_log.error("unexpected problem", e); // should be sorted in loop
				}
				// alter location
				location.setDestination(get.getPO());
			} finally {
				// release write lock on location
				locationLock.release();
			}
		}
		
		// called on SO...
		public void onMessage(ObjectMessage message1, GetBOToSO get) {
			Serializable key=get.getKey();
			// lock map [partition]
			synchronized (_map) { // naive
				// send GetSOToPO to PO
				Destination so=_cluster.getLocalNode().getDestination();
				Destination po=get.getPO();
				GetSOToPO request=new GetSOToPO((Serializable)_map.get(key));
				ObjectMessage message2=(ObjectMessage)_dispatcher.exchangeSend(so, po, request, 2000L, get.getPOCorrelationId());
				// wait
				// receive GetPOToSO
				GetPOToSO response=null;
				try {
					response=(GetPOToSO)message2.getObject();
				} catch (JMSException e) {
					_log.error("unexpected problem", e);
				}
				// remove association
				_map.remove(key);
				// unlock map [partition]
			}

			// send GetSOToBO to BO
			Destination bo=get.getBO();
			_dispatcher.reply(message1,new GetSOToBO());
		}
		
		
		//--------------------------------------------------------------------------------
		// Put
		//--------------------------------------------------------------------------------

		// called on PO...
		/* (non-Javadoc)
		 * @see org.codehaus.wadi.sandbox.gridstate.Protocol#put(java.io.Serializable, java.io.Serializable, boolean, boolean)
		 */
		public Serializable put(Serializable key, Serializable value, boolean overwrite, boolean returnOldValue) {
			// lock map [partition]
			synchronized (_map) { // naive - should only lock map partition...
				Serializable oldValue=(Serializable)_map.put(key, value);
				if (oldValue!=null)
					if (!overwrite) {
						_map.put(key, oldValue);
						return Boolean.FALSE;
					} else
						return returnOldValue?oldValue:null; // we are PO and SO, so BO already has correct location...
				else {
					// exchangeSendLoop PutPOToBO to BO
					Destination po=_cluster.getLocalNode().getDestination();
					Destination bo=_buckets[_mapper.map(key)].getDestination();
					PutPOToBO request=new PutPOToBO(key, overwrite, returnOldValue, po);
					ObjectMessage message=_dispatcher.exchangeSendLoop(po, bo, request, 2000L);
					Serializable response=null;
					try {
						response=message.getObject();
					} catch (JMSException e) {
						_log.error("unexpected problem", e); // should be in loop - TODO
					}
					
					// 2 possibilities - 
					// PutBO2PO (first time this key has been used...
					if (response instanceof PutBOToPO) {
						// therefore old value was null
						if (!overwrite)
							return ((PutBOToPO)response).getSuccess()?Boolean.TRUE:Boolean.FALSE;
						else
							return null;
					} else if (response instanceof PutSOToPO) {
						// key was already associated with some value...
						return ((PutSOToPO)response).getValue();
					} else {
						_log.error("unexpected response: "+response.getClass().getName());
						return null;
					}

					// release map [partition]
				}
			}
		}

		// called on BO...
		public void onMessage(ObjectMessage message1, PutPOToBO put) {
			// what if we are NOT the BO anymore ?
			Serializable key=put.getKey();
			Bucket bucket=_buckets[_mapper.map(key)];
			Map bucketMap=bucket.getMap();
			Sync bucketLock=bucket.getLock().writeLock();
			try {
				// get write lock on bucket
				Utils.safeAcquire(bucketLock);
				Location location=new Location(put.getPO());
				Location oldLocation=(Location)bucketMap.put(key, location);
				if (oldLocation==null) {
					// no previous value
					// send BOToPO - success
					_dispatcher.reply(message1, new PutBOToPO(true));
				} else if (!put.getOverwrite()) {
					// we shouldn't have overwritten...
					boolean success;
					if (oldLocation.getDestination().equals(location.getDestination())) {
						// location has not changed, so it does not matter...
						success=true;
					} else {
						// replace oldLocation
						bucketMap.put(key, oldLocation);
						success=false;
					}
					// send BOToPO - failure
					_dispatcher.reply(message1, new PutBOToPO(success));
				} else {
					// previous value needs removing and possibly returning...
					// send BOToSO...
					throw new UnsupportedOperationException("NYI");
				}
				
			} finally {
				bucketLock.release();
			}
		}
		
		//--------------------------------------------------------------------------------
		// Remove
		//--------------------------------------------------------------------------------

		public Serializable remove(Serializable key, boolean returnOldValue) {
			throw new UnsupportedOperationException("NYI");
		}

	}
	
  /*
   * second pass
   */
  public boolean containsKey(Object key) {
	  throw new UnsupportedOperationException();
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
	  return ((Boolean)put(key, value, false, false)).booleanValue();
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
  
}
