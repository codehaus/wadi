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
import org.codehaus.wadi.sandbox.gridstate.messages.PutAbsentCommit;
import org.codehaus.wadi.sandbox.gridstate.messages.PutAbsentBegin;
import org.codehaus.wadi.sandbox.gridstate.messages.PutAbsentBegun;
import org.codehaus.wadi.sandbox.gridstate.messages.PutAbsentRollback;
import org.codehaus.wadi.sandbox.gridstate.messages.PutRequest;



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
	
	public GCache(String nodeName, int numBuckets, Dispatcher dispatcher, BucketMapper mapper) {
		_buckets=new Bucket[numBuckets];
		_dispatcher=dispatcher;
		_cluster=_dispatcher.getCluster();
		_protocol=new Protocol();
		_mapper=mapper;
		
		for (int i=0; i<numBuckets; i++) {
			Bucket bucket=new Bucket(new LocalBucket());
			bucket.init(this);
			_buckets[i]=bucket;
		}
	}
	
	public class Protocol {
		
		public Protocol() {
			_dispatcher.register(this, "onMessage", PutAbsentBegin.class);
			_dispatcher.register(PutAbsentBegun.class, 2000);
			_dispatcher.register(PutAbsentCommit.class, 2000);
			_dispatcher.register(PutAbsentRollback.class, 2000);
		}
		
		public boolean putAbsent(Serializable key, Serializable value, Map map) {
			// should take non-exclusive lock on bucket array... - // TODO
			Bucket bucket=_buckets[_mapper.map(key)];
			Destination destination=_cluster.getLocalNode().getDestination();
			Conversation conversation=new Conversation();
			boolean success=bucket.putAbsentBegin(conversation, key, destination);
			if (success) {
				Object oldValue=map.put(key, value);
				assert oldValue==null;
				bucket.putAbsentCommit(conversation, key, destination);
			}
			else {
				bucket.putAbsentRollback(conversation, key, destination);
			}
			
			return success; 
		}
		
		public void putExists(Serializable key) {
			_buckets[_mapper.map(key)].putExists(key, _cluster.getLocalNode().getDestination());
		}
		
		public Serializable removeReturn(Serializable key, Map map) {
			return _buckets[_mapper.map(key)].removeReturn(key, map);
		}

		public void removeNoReturn(Serializable key) {
			_buckets[_mapper.map(key)].removeNoReturn(key);
		}
		
		public void onMessage(ObjectMessage message, PutAbsentBegin request) {
			Serializable key=request.getKey();
			Destination destination=request.getDestination();
			// should take read lock on bucket array... - TODO
			Bucket bucket=_buckets[_mapper.map(key)];
			Conversation conversation=new Conversation();
			boolean success=bucket.putAbsentBegin(conversation, key, destination);
			ObjectMessage reply=_dispatcher.exchangeReplyLoop(message, new PutAbsentBegun(success), 2000);
			Object tmp=null;
			try {
				tmp=reply.getObject();
			} catch (JMSException e) { // should be included in loop - TODO
				_log.error("unexpected problem", e);
			}
			if (tmp instanceof PutAbsentCommit) {
				PutAbsentCommit commit=(PutAbsentCommit)tmp;
				bucket.putAbsentCommit(conversation, commit.getKey(), destination);
			} else if (tmp instanceof PutAbsentRollback) {
				PutAbsentRollback rollback=(PutAbsentRollback)tmp;
				bucket.putAbsentRollback(conversation, rollback.getKey(), destination);
			} else {
				_log.error("unexpected message type: "+tmp.getClass().getName());
			}
		}
		
		public void destroy(String key) {
			
		}
		
		public void fetch(String key) {
			
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
	  return _map.get(key);
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
  protected Map _map=new HashMap();
  
  public Object put(Object key, Object value) {
	  _protocol.putExists((Serializable)key);
	  //_dindex.insert((String)key);
	  // should do a fetch here ? - TODO
	  return _map.put(key, value);
  }

  // for WADI
  public boolean putAbsent(Object key, Object value) {
	  return _protocol.putAbsent((Serializable)key, (Serializable)value, _map);
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
	  return _protocol.removeReturn((Serializable)key, _map);
  }

  public void removeNoReturn(Object key) {
	  _protocol.removeNoReturn((Serializable)key);
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
