/**
 * 
 */

package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.Message;

import org.activecluster.LocalNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.Dispatcher;
import org.codehaus.wadi.sandbox.gridstate.messages.MovePOToSO;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveSOToBO;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveSOToPO;
import org.codehaus.wadi.sandbox.gridstate.messages.ReadBOToPO;
import org.codehaus.wadi.sandbox.gridstate.messages.WriteBOToPO;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.MessageListener;
import org.jgroups.SuspectedException;
import org.jgroups.TimeoutException;
import org.jgroups.View;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.blocks.RpcDispatcher;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public class JGroupsIndirectProtocol implements Protocol , BucketConfig {
	
	protected final Log _log=LogFactory.getLog(getClass());
	protected final String _clusterName="ORG.CODEHAUS.WADI.TEST";
	protected final long _timeout=30*1000L;
	protected final Bucket[] _buckets;
	
	protected final Channel _channel;
	protected RpcDispatcher _dispatcher;
	protected Address _address;
	protected final Map _rvMap=new HashMap();
	
	protected final MembershipListener _membershipListener=new MembershipListener() {
		
		public void viewAccepted(View arg0) {
			// TODO Auto-generated method stub
			
		}
		
		public void suspect(Address arg0) {
			// TODO Auto-generated method stub
			
		}
		
		public void block() {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	protected final MessageListener _messageListener=new MessageListener() {

		public void receive(org.jgroups.Message arg0) {
			// TODO Auto-generated method stub
			
		}

		public byte[] getState() {
			// TODO Auto-generated method stub
			return null;
		}

		public void setState(byte[] arg0) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	
	public JGroupsIndirectProtocol(String nodeName, int numBuckets, BucketMapper mapper) throws Exception {
		
		_buckets=new Bucket[numBuckets];
		
		for (int i=0; i<numBuckets; i++) {
			Bucket bucket=new Bucket(new LocalBucket());
			bucket.init(this);
			_buckets[i]=bucket;
		}
		
		_channel=new JChannel();
		_dispatcher=new RpcDispatcher(_channel, _messageListener, _membershipListener, (Object)this, true, true);
	}
	
	protected ProtocolConfig _config;
	
	public void init(ProtocolConfig config) {
		_config=config;
		String channelName="WADI";
		try {
		_channel.connect(channelName);
		} catch (Exception e) {
			_log.error("ohoh!", e);
		}
		_address=_channel.getLocalAddress();
	}
	
	public BucketInterface createRemoteBucket() {
		return new JGroupsRemoteBucket(_address);
	}
	
	
	public void start() throws Exception {
		_log.debug("starting....");
		_dispatcher.start();
	}
	
	public void stop() throws Exception {
	      _dispatcher.stop();
	      _channel.disconnect();
	}
	
	
	public Bucket[] getBuckets() {
		return _buckets;
	}
	
	// BucketConfig
	
	public Destination getLocalDestination() {
		throw new UnsupportedOperationException("bah");
	}

	public Address getLocalAddress() {
		return _address;
	}
	
	public Dispatcher getDispatcher() {
		throw new UnsupportedOperationException("bah");
	}
	
	protected Object
	rpc(Address address, String methodName, Class[] argClasses, Object[] argInstances) throws TimeoutException, SuspectedException
	{
		_log.info("rpc-ing from:"+_address+" to:"+address);
		if (address==null)
			throw new NullPointerException();
		return _dispatcher.callRemoteMethod(address, "dispatch", new Object[]{methodName, argClasses, argInstances}, new Class[]{String.class, Class[].class, Object[].class}, GroupRequest.GET_ALL, _timeout);
	}
	
	public Object
	dispatch(String methodName, Class[] argClasses, Object[] argInstances)
	{
		_log.info("methodName: "+methodName);
		for (int i=0; i<argClasses.length; i++)
			_log.info("type: "+argClasses[i]+" : "+argInstances[i]);
		
		try {
			return getClass().getMethod(methodName, argClasses).invoke(this, argInstances);
		} catch (Exception e) {
			_log.error("something went wrong", e);
			return null;
		}
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
			_log.info("get - [PO] trying for lock("+key+")...");
			sync=_config.getSOSyncs().acquire(key);
			_log.info("get - [PO] ...lock("+key+") acquired - "+sync);
			Serializable value=null;
			Map map=_config.getMap();
			synchronized (map) {
				value=(Serializable)map.get(key);
			}
			if (value!=null)
				return value;
			else {
				// exchangeSendLoop GetPOToBO to BO
				Object response=null;
				try {
					Address po=_address;
					Address bo=_buckets[_config.getBucketMapper().map(key)].getAddress();
					response=rpc(bo, "dispatchReadPOToBO", new Class[]{Object.class, Address.class}, new Object[]{key, po});
 				} catch(Exception e) {
					_log.error("problem publishing change in state over JavaGroups", e);
				}
				
				if (response instanceof ReadBOToPO) {
					// association not present
					value=null;
				} else if (response instanceof Boolean) {
					_log.info("get "+(((Boolean)response).booleanValue()?"succeeded":"failed"));
					synchronized (_rvMap) {
						value=(Serializable)_rvMap.remove(key);
						_log.info("getting: "+key+"="+value+ " - "+this);
						synchronized (map) {
							map.put(key, value);
						}
					}
					return value;
				}
				
				return value;
			}
		} finally {
			_log.info("get - [PO] releasing lock("+key+") - "+sync);
			sync.release();
		}
	}
	
	public Object dispatchMoveSOToPO(Object key, Object value) {
		_log.info("[PO] - dispatchMoveSOToPO@"+_address);
		// association exists
		// associate returned value with key
		//_log.info("received "+key+"="+value+" <- SO");
		//Map map=_config.getMap();
		_log.info("putting: "+key+"="+value+ " - "+this+" localAddress:"+_address);
		if (value!=null) {
			synchronized (_rvMap) {
				_rvMap.put(key, value);
			}
		}
		return new MovePOToSO(true);
	}
	
	// called on BO...
	public Object dispatchReadPOToBO(Object key, Address po) throws SuspectedException, TimeoutException {
		_log.info("po="+po);
		// what if we are NOT the BO anymore ?
		// get write lock on location
		Sync sync=null;
		try {
			_log.info("dispatchReadPOToBO- [BO] trying for lock("+key+")...");
			sync=_config.getBOSyncs().acquire((Serializable)key);
			_log.info("dispatchReadPOToBO- [BO] ...lock("+key+") acquired - "+sync);
			Bucket bucket=_buckets[_config.getBucketMapper().map((Serializable)key)];
			JGroupsLocation location=(JGroupsLocation)bucket.getLocation((Serializable)key);
			if (location==null) {
				return new ReadBOToPO();
			}
			// exchangeSendLoop GetBOToSO to SO
			Address bo=_address;
			Address so=location.getAddress();
			
			MoveSOToBO response=(MoveSOToBO)rpc(so, "dispatchMoveBOToSO", new Class[]{Object.class, Address.class, Address.class}, new Object[]{key, po, bo});
			// success - update location
			boolean success=response.getSuccess();
			if (success)
				location.setAddress(po);
			
			return success?Boolean.TRUE:Boolean.FALSE; 
		} finally {
			_log.info("dispatchReadPOToBO- [BO] releasing lock("+key+") - "+sync);
			sync.release();
		}	
	}
	
	// called on SO...
	public Object dispatchMoveBOToSO(Object key, Address po, Address bo) throws SuspectedException, TimeoutException {
		_log.info("[SO] - dispatchMoveBOToSO@"+_address);
		_log.info("po="+po);
		Sync sync=null;
		try {
			_log.info("dispatchMoveBOToSO - [SO] trying for lock("+key+")...");
			sync=_config.getSOSyncs().acquire((Serializable)key);
			_log.info("dispatchMoveBOToSO - [SO] ...lock("+key+") acquired< - "+sync);
			// send GetSOToPO to PO
			Object value;
			Map map=_config.getMap();
			synchronized (map) {
				value=(Serializable)map.get(key);
			}
			_log.info("[SO] sending "+key+"="+value+" -> PO...");
			MovePOToSO response=(MovePOToSO)rpc(po, "dispatchMoveSOToPO", new Class[]{Object.class, Object.class}, new Object[]{key, value});
			_log.info("[SO] ...response received <- PO");
			boolean success=response.getSuccess();
			if (success) {
				synchronized (map) {
					map.remove(key);
					return new MoveSOToBO();
				}
			}
			return new MoveSOToBO(success);
		} finally {
			_log.info("dispatchMoveBOToSO - [SO] releasing lock("+key+") - "+sync);
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
	public Serializable put(Serializable key, Serializable newValue, boolean overwrite, boolean returnOldValue) {
		_log.info("[PO] - put@"+_address);
		boolean removal=(newValue==null);
		Map map=_config.getMap();
		Sync sync=null;
		try {
			_log.info("put- [PO] trying for lock("+key+")...");
			sync=_config.getSOSyncs().acquire(key);
			_log.info("put- [PO] ...lock("+key+") acquired - "+sync);
			
			if (!removal) { // removals must do the round trip to BO
				boolean local;
				synchronized (map) {
					local=map.containsKey(key);
				}
				
				if (local) {
					// local
					if (overwrite) {
						synchronized (map) {
							Serializable oldValue=(Serializable)map.put(key, newValue);
							return returnOldValue?oldValue:null;
						}
					} else {
						return Boolean.FALSE;
					}
				}
			}
			
			try {
				// absent or remote
				// exchangeSendLoop PutPOToBO to BO
				Address po=_address;
				Address bo=_buckets[_config.getBucketMapper().map(key)].getAddress();
				Object response=rpc(bo, "dispatchWritePOToBO",
						new Class[]{Object.class, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE, Address.class},
						new Object[]{key, newValue==null?Boolean.TRUE:Boolean.FALSE, overwrite?Boolean.TRUE:Boolean.FALSE, returnOldValue?Boolean.TRUE:Boolean.FALSE, po});
				
				// 2 possibilities - 
				// PutBO2PO - Absent
				if (response instanceof WriteBOToPO) {
					if (overwrite) {
						synchronized (map) {
							Serializable oldValue=(Serializable)(removal?map.remove(key):map.put(key, newValue));
							return returnOldValue?oldValue:null;
						}
					} else {
						if (((WriteBOToPO)response).getSuccess()) {
							synchronized (map) {
								map.put(key, newValue);
							}
							return Boolean.TRUE;
						} else {
							return Boolean.FALSE;
						}
					}
				} else if (response instanceof Boolean) {
					boolean success=((Boolean)response).booleanValue();
					if (returnOldValue && success) {
						Object oldValue=null;
						synchronized (_rvMap) {
							oldValue=_rvMap.remove(key);
							_log.info("getting: "+key+"="+oldValue+ " - "+this+" localAddress:"+_address);
							if (!removal && oldValue!=null) {
								synchronized (map) {
									map.put(key, newValue);
								}
							}
						}
						return (Serializable)oldValue;
					} else
						return null;
				} else {
					_log.error("unexpected response: "+response.getClass().getName());
					return null;
				}
			} catch (Exception e) {
				_log.error("something went wrong :-(", e);
				return null;
			}
		} finally {
			_log.info("put- [PO] releasing lock("+key+") - "+sync);
			sync.release();
		}
	}
	
	// called on BO...
	public Object dispatchWritePOToBO(Object key, boolean valueIsNull, boolean overwrite, boolean returnOldValue, Address po) throws SuspectedException, TimeoutException {
		_log.info("[BO] - dispatchWritePOToBO@"+_address);
		// what if we are NOT the BO anymore ?
		Bucket bucket=_buckets[_config.getBucketMapper().map((Serializable)key)];
		Map bucketMap=bucket.getMap();
		Sync sync=null;
		try {
			_log.info("dispatchWritePOToBO- [BO] trying for lock("+key+")...");
			sync=_config.getBOSyncs().acquire((Serializable)key);
			_log.info("dispatchWritePOToBO- [BO] ...lock("+key+") acquired - "+sync);
			Location location=valueIsNull?null:new JGroupsLocation(po);
			// remove or update location, remembering old value
			JGroupsLocation oldLocation=(JGroupsLocation)(location==null?bucketMap.remove(key):bucketMap.put(key, location));
			// if we are not allowed to overwrite, and we have...
			if (!overwrite && oldLocation!=null) {
				//  undo our change
				bucketMap.put(key, oldLocation);
				// send BOToPO - failure
				return new WriteBOToPO(false);
			} else if (oldLocation==null || (po.equals(oldLocation.getAddress()))) {
				// if there was previously no SO, or there was, but it was PO ...
				// then there is no need to go and remove the old value from the old SO
				// send BOToPO - success
				return new WriteBOToPO(true);
			} else {
				// previous value needs removing and possibly returning...
				// send BOToSO...
				Address bo=_address;
				Address so=oldLocation.getAddress();
				_log.info(""+po+"=="+so+" ? "+(po.equals(so)));
				MoveSOToBO response=(MoveSOToBO)rpc(so, "dispatchMoveBOToSO", new Class[]{Object.class, Address.class, Address.class}, new Object[]{key, po, bo});
				return response.getSuccess()?Boolean.TRUE:Boolean.FALSE;
			}
		} finally {
			_log.info("dispatchWritePOToBO- [BO] releasing lock("+key+") - "+sync);
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