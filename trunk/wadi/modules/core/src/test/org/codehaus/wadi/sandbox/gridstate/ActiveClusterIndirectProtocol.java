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
package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.activecluster.Node;
import org.activemq.ActiveMQConnectionFactory;
import org.activemq.store.vm.VMPersistenceAdapterFactory;
import org.codehaus.wadi.ActiveClusterDispatcherConfig;
import org.codehaus.wadi.Dispatcher;
import org.codehaus.wadi.ExtendedCluster;
import org.codehaus.wadi.impl.CustomClusterFactory;
import org.codehaus.wadi.impl.ActiveClusterDispatcher;
import org.codehaus.wadi.sandbox.gridstate.messages.MovePMToSM;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveIMToSM;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveSMToPM;
import org.codehaus.wadi.sandbox.gridstate.messages.MoveSMToIM;
import org.codehaus.wadi.sandbox.gridstate.messages.ReadPMToIM;
import org.codehaus.wadi.sandbox.gridstate.messages.ReadIMToPM;
import org.codehaus.wadi.sandbox.gridstate.messages.WritePMToIM;
import org.codehaus.wadi.sandbox.gridstate.messages.WriteIMToPM;
import org.jgroups.Address;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public class ActiveClusterIndirectProtocol extends AbstractIndirectProtocol {

	//protected final String _clusterUri="peer://org.codehaus.wadi";
	//protected final String _clusterUri="tcp://smilodon:61616";
	protected final String _clusterUri="vm://localhost";
	protected final ActiveMQConnectionFactory _connectionFactory=new ActiveMQConnectionFactory(_clusterUri);
	protected final CustomClusterFactory _clusterFactory=new CustomClusterFactory(_connectionFactory);
	protected final Cluster _cluster;

	class MyDispatcherConfig implements ActiveClusterDispatcherConfig {

		protected final Cluster _cluster;

		MyDispatcherConfig(Cluster cluster) {
			_cluster=cluster;
		}

		public ExtendedCluster getCluster() {
			return (ExtendedCluster)_cluster;
		}
	}

	public ActiveClusterIndirectProtocol(String nodeName, PartitionManager manager, PartitionMapper mapper, long timeout) throws Exception {
		super(nodeName, manager, mapper, timeout, null);
		System.setProperty("activemq.persistenceAdapterFactory", VMPersistenceAdapterFactory.class.getName());
		//_clusterFactory.setInactiveTime(100000L); // ???
		_cluster=_clusterFactory.createCluster(_clusterName);
		_dispatcher=new ActiveClusterDispatcher(nodeName);
		_dispatcher.init(new MyDispatcherConfig(_cluster));


		// Get - 5 messages - IM->PM->SM->IM->SM->PM
		_dispatcher.register(this, "onMessage", ReadIMToPM.class);
//		_dispatcher.newRegister(this, "onReadIMToPM", ReadIMToPM.class);
		_dispatcher.register(this, "onMessage", MovePMToSM.class);
//		_dispatcher.newRegister(this, "onMovePMToSM", MovePMToSM.class);
		_dispatcher.register(MoveSMToIM.class, _timeout);
		_dispatcher.register(MoveIMToSM.class, _timeout);
		_dispatcher.register(MoveSMToPM.class, _timeout);
		// Get - 2 messages - IM->PM->IM (NYI)
		_dispatcher.register(ReadPMToIM.class, _timeout);

		// Put - 2 messages - IM->PM->IM
		_dispatcher.register(this, "onMessage", WriteIMToPM.class);
		_dispatcher.register(WritePMToIM.class, _timeout);
	}

	public PartitionInterface createRemotePartition() {
		return new ActiveClusterRemotePartition(_cluster.getLocalNode().getDestination());
	}


	public void start() throws Exception {
		Map state=new HashMap();
		state.put("nodeName", _nodeName);
		_cluster.getLocalNode().setState(state);
		_cluster.start();
	}

	public void stop() throws Exception {
		_cluster.stop();
	}


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

	// called on IM...
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.gridstate.Protocol#get(java.io.Object)
	 */
	public Object get(Object key) {
		Sync sync=null;
		String agent=_nodeName;
		try {
			_log.trace("["+agent+"@"+_nodeName+"(IM)] - "+key+" - acquiring sync("+sync+")..."+" <"+Thread.currentThread().getName()+">");
			sync=_config.getSMSyncs().acquire(key); // TODO - an SMSync should actually be a lock in the state itself - read or write ?
			_log.trace("["+agent+"@"+_nodeName+"(IM)] - "+key+" - ...sync("+sync+") acquired"+" <"+Thread.currentThread().getName()+">");
			Object value=null;
			Map map=_config.getMap();
			synchronized (map) {
				value=map.get(key);
			}
			if (value!=null)
				return value;
			else {
				// exchangeSendLoop GetIMToPM to PM
				Destination im=_cluster.getLocalNode().getDestination();
				Destination pm=_partitionManager.getPartitions()[_config.getPartitionMapper().map(key)].getDestination();
				ReadIMToPM request=new ReadIMToPM(key, im);
				ObjectMessage message=_dispatcher.exchangeSendLoop(im, pm, request, _timeout, 10);
				Object response=null;
				try {
					response=message.getObject();
				} catch (JMSException e) {
					_log.error("unexpected problem", e); // should be in loop - TODO
				}

				if (response instanceof ReadPMToIM) {
					// association not present
					value=null;
				} else if (response instanceof MoveSMToIM) {
					// association exists
					// associate returned value with key
					value=((MoveSMToIM)response).getValue();
					//_log.info("received "+key+"="+value+" <- SM");
					synchronized (_config.getMap()) {
						map.put(key, value);
					}
					// reply GetIMToSM to SM
					_dispatcher.reply(message, new MoveIMToSM());
				}

				return value;
			}
		} finally {
			_log.trace("["+agent+"@"+_nodeName+"(IM)] - "+key+" - releasing sync("+sync+")..."+" <"+Thread.currentThread().getName()+">");
			sync.release();
			_log.trace("["+agent+"@"+_nodeName+"(IM)] - "+key+" - ...sync("+sync+") released"+" <"+Thread.currentThread().getName()+">");
		}
	}

	// called on PM...
	public void onMessage(ObjectMessage message1, ReadIMToPM get) {
		// what if we are NOT the PM anymore ?
		// get write lock on location
		Object key=get.getKey();
		Sync sync=null;
		String agent=getNodeName((Destination)get.getIM());
		try {
			_log.trace("["+agent+"@"+_nodeName+"(PM)] - "+key+" - acquiring sync("+sync+")..."+" <"+Thread.currentThread().getName()+">");
			sync=_config.getPMSyncs().acquire(key); // TODO - PMSyncs are actually WLocks on a given sessions location (partition entry) - itegrate
			_log.trace("["+agent+"@"+_nodeName+"(PM)] - "+key+" - ...sync("+sync+") acquired"+" <"+Thread.currentThread().getName()+">");
			Partition partition=_partitionManager.getPartitions()[_config.getPartitionMapper().map(key)];
			ActiveClusterLocation location=(ActiveClusterLocation)partition.getLocation(key);
			if (location==null) {
				_dispatcher.reply(message1,new ReadPMToIM());
				return;
			}
			// exchangeSendLoop GetPMToSM to SM
			Destination im=(Destination)get.getIM();
			Destination pm=_cluster.getLocalNode().getDestination();
			Destination sm=(Destination)location.getValue();
			String poCorrelationId=null;
			try {
				poCorrelationId=ActiveClusterDispatcher.getOutgoingCorrelationId(message1);
				//_log.info("Process Owner Correlation ID: "+poCorrelationId);
			} catch (JMSException e) {
				_log.error("unexpected problem", e);
			}
			MovePMToSM request=new MovePMToSM(key, im, pm, poCorrelationId);
			ObjectMessage message2=_dispatcher.exchangeSendLoop(pm, sm, request, _timeout, 10);
			if (message2==null)
				_log.error("NO RESPONSE WITHIN TIMEFRAME - PANIC!");

			MoveSMToPM response=null;
			try {
				response=(MoveSMToPM)message2.getObject();
			} catch (JMSException e) {
				_log.error("unexpected problem", e); // should be sorted in loop
			}
			// alter location
			location.setValue((Destination)get.getIM());

		} finally {
			_log.trace("["+agent+"@"+_nodeName+"(PM)] - "+key+" - releasing sync("+sync+")..."+" <"+Thread.currentThread().getName()+">");
			sync.release();
			_log.trace("["+agent+"@"+_nodeName+"(PM)] - "+key+" - ...sync("+sync+") released"+" <"+Thread.currentThread().getName()+">");
		}
	}

	// called on SM...
	public void onMessage(ObjectMessage message1, MovePMToSM get) {
		Object key=get.getKey();
		String agent=getNodeName((Destination)get.getIM());
		Sync sync=null;
		try {
			_log.trace("["+agent+"@"+_nodeName+"(SM)] - "+key+" - acquiring sync("+sync+")..."+" <"+Thread.currentThread().getName()+">");
			sync=_config.getSMSyncs().acquire(key);
			_log.trace("["+agent+"@"+_nodeName+"(SM)] - "+key+" - ...sync("+sync+") acquired"+" <"+Thread.currentThread().getName()+">");
			// send GetSMToIM to IM
			Destination sm=_cluster.getLocalNode().getDestination();
			Destination im=(Destination)get.getIM();
			Object value;
			Map map=_config.getMap();
			synchronized (map) {
				value=map.get(key);
			}
			//_log.info("sending "+key+"="+value+" -> IM");
			MoveSMToIM request=new MoveSMToIM(key, value);
			ObjectMessage message2=(ObjectMessage)_dispatcher.exchangeSend(sm, im, request, _timeout, get.getIMCorrelationId());
			// wait
			// receive GetIMToSM

			if (message2==null) {
				_log.error("NO REPLY RECEIVED FOR MESSAGE IN TIMEFRAME - PANIC!");
			} else {
			}
			MoveIMToSM response=null;
			try {
				response=(MoveIMToSM)message2.getObject();
				// remove association
				synchronized (map) {
					map.remove(key);
				}
				// send GetSMToPM to PM
				//Destination pm=(Destination)get.getPM();
				_dispatcher.reply(message1,new MoveSMToPM());
			} catch (JMSException e) {
				_log.error("unexpected problem", e);
			}
		} finally {
			_log.trace("["+agent+"@"+_nodeName+"(SM)] - "+key+" - releasing sync("+sync+")..."+" <"+Thread.currentThread().getName()+">");
			sync.release();
			_log.trace("["+agent+"@"+_nodeName+"(SM)] - "+key+" - ...sync("+sync+") released"+" <"+Thread.currentThread().getName()+">");
		}
	}


	//--------------------------------------------------------------------------------
	// Put
	//--------------------------------------------------------------------------------

	// called on IM...
	/* (non-Javadoc)
	 * @see org.codehaus.wadi.sandbox.gridstate.Protocol#put(java.io.Object, java.io.Object, boolean, boolean)
	 */
	public Object put(Object key, Object value, boolean overwrite, boolean returnOldValue) {
		boolean removal=(value==null);
		Map map=_config.getMap();
		Sync sync=null;
		String agent=_nodeName;
		try {
			_log.trace("["+agent+"@"+_nodeName+"(IM)] - "+key+" - acquiring sync("+sync+")..."+" <"+Thread.currentThread().getName()+">");
			sync=_config.getSMSyncs().acquire(key);
			_log.trace("["+agent+"@"+_nodeName+"(IM)] - "+key+" - ...sync("+sync+") acquired"+" <"+Thread.currentThread().getName()+">");

			if (!removal) { // removals must do the round trip to PM
				boolean local;
				synchronized (map) {
					local=map.containsKey(key);
				}

				if (local) {
					// local
					if (overwrite) {
						synchronized (map) {
							Object oldValue=map.put(key, value);
							return returnOldValue?oldValue:null;
						}
					} else {
						return Boolean.FALSE;
					}
				}
			}

			// absent or remote
			// exchangeSendLoop PutIMToPM to PM
			Destination im=_cluster.getLocalNode().getDestination();
			Destination pm=_partitionManager.getPartitions()[_config.getPartitionMapper().map(key)].getDestination();
			WriteIMToPM request=new WriteIMToPM(key, value==null, overwrite, returnOldValue, im);
			ObjectMessage message=_dispatcher.exchangeSendLoop(im, pm, request, _timeout, 10);
			Object response=null;
			try {
				response=message.getObject();
			} catch (JMSException e) {
				_log.error("unexpected problem", e); // should be in loop - TODO
			}

			// 2 possibilities -
			// PutPM2IM - Absent
			if (response instanceof WritePMToIM) {
				if (overwrite) {
					synchronized (map) {
						Object oldValue=(removal?map.remove(key):map.put(key, value));
						return returnOldValue?oldValue:null;
					}
				} else {
					if (((WritePMToIM)response).getSuccess()) {
						synchronized (map) {
							map.put(key, value);
						}
						return Boolean.TRUE;
					} else {
						return Boolean.FALSE;
					}
				}
			} else if (response instanceof MoveSMToIM) {
				// Present - remote
				// reply GetIMToSM to SM
				_dispatcher.reply(message, new MoveIMToSM());
				synchronized (map) {
					if (removal)
						map.remove(key);
					else
						map.put(key, value);
				}
				return ((MoveSMToIM)response).getValue();
			} else {
				_log.error("unexpected response: "+response.getClass().getName());
				return null;
			}

		} finally {
			_log.trace("["+agent+"@"+_nodeName+"(IM)] - "+key+" - releasing sync("+sync+")..."+" <"+Thread.currentThread().getName()+">");
			sync.release();
			_log.trace("["+agent+"@"+_nodeName+"(IM)] - "+key+" - ...sync("+sync+") released"+" <"+Thread.currentThread().getName()+">");
		}
	}

	// called on PM...
	public void onMessage(ObjectMessage message1, WriteIMToPM write) {
		// what if we are NOT the PM anymore ?
		Object key=write.getKey();
		Partition partition=_partitionManager.getPartitions()[_config.getPartitionMapper().map(key)];
		Map partitionMap=partition.getMap();
		Sync sync=null;
		String agent=getNodeName((Destination)write.getIM());
		try {
			_log.trace("["+agent+"@"+_nodeName+"(PM)] - "+key+" - acquiring sync("+sync+")..."+" <"+Thread.currentThread().getName()+">");
			sync=_config.getPMSyncs().acquire(key);
			_log.trace("["+agent+"@"+_nodeName+"(PM)] - "+key+" - ...sync("+sync+") acquired"+" <"+Thread.currentThread().getName()+">");
			Location location=write.getValueIsNull()?null:new ActiveClusterLocation((Destination)write.getIM());
			// remove or update location, remembering old value
			ActiveClusterLocation oldLocation=(ActiveClusterLocation)(location==null?partitionMap.remove(key):partitionMap.put(key, location));
			// if we are not allowed to overwrite, and we have...
			if (!write.getOverwrite() && oldLocation!=null) {
				//  undo our change
				partitionMap.put(key, oldLocation);
				// send PMToIM - failure
				_dispatcher.reply(message1, new WritePMToIM(false));
			} else if (oldLocation==null || (write.getIM().equals(oldLocation.getValue()))) {
				// if there was previously no SM, or there was, but it was IM ...
				// then there is no need to go and remove the old value from the old SM
				// send PMToIM - success
				_dispatcher.reply(message1, new WritePMToIM(true));
			} else {
				// previous value needs removing and possibly returning...
				// send PMToSM...

				String poCorrelationId=null;
				try {
					poCorrelationId=ActiveClusterDispatcher.getOutgoingCorrelationId(message1);
					//_log.info("Process Owner Correlation ID: "+poCorrelationId);
				} catch (JMSException e) {
					_log.error("unexpected problem", e);
				}
				Destination im=(Destination)write.getIM();
				Destination pm=_cluster.getLocalNode().getDestination();
				Destination sm=(Destination)oldLocation.getValue();
				MovePMToSM request=new MovePMToSM(key, im, pm, poCorrelationId);
				/*ObjectMessage message2=*/_dispatcher.exchangeSendLoop(pm, sm, request, _timeout, 10);
//				MoveSMToPM response=null;
//				try {
//				response=(MoveSMToPM)message2.getObject();
//				} catch (JMSException e) {
//				_log.error("unexpected problem", e); // should be sorted in loop
//				}
			}

		} finally {
			_log.trace("["+agent+"@"+_nodeName+"(PM)] - "+key+" - releasing sync("+sync+")..."+" <"+Thread.currentThread().getName()+">");
			sync.release();
			_log.trace("["+agent+"@"+_nodeName+"(PM)] - "+key+" - ...sync("+sync+") released"+" <"+Thread.currentThread().getName()+">");
		}
	}

	//--------------------------------------------------------------------------------
	// Remove
	//--------------------------------------------------------------------------------

	// called on IM...
	public Object remove(Object key, boolean returnOldValue) {
		return put(key, null, true, returnOldValue); // a remove is a put(key, null)...
	}

	//--------------------------------------------------------------------------------
	// Protocol
	//--------------------------------------------------------------------------------

	public Object syncRpc(Object destination, String methodName, Object message) throws Exception {
		ObjectMessage tmp=_dispatcher.exchangeSendLoop(_cluster.getLocalNode().getDestination(), (Destination)destination, (Serializable)message, _timeout, 10);
		Object response=null;
		try {
			response=tmp.getObject();
		} catch (JMSException e) {
			_log.error("unexpected problem", e); // should be in loop - TODO
		}
		return response;
	}

	public Object getLocalLocation() {
		return _cluster.getLocalNode().getDestination();
	}


	protected String getNodeName(Destination destination) {
		Node node;
		if (destination.equals(_cluster.getLocalNode().getDestination()))
			node=_cluster.getLocalNode();
		else
			node=(Node)_cluster.getNodes().get(destination);

		Map state=node.getState();
		String name=(String)state.get("nodeName");
		return name;
	}

}
