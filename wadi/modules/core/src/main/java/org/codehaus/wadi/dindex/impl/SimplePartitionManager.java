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
import java.util.Iterator;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.activecluster.Cluster;
import org.apache.activecluster.ClusterEvent;
import org.apache.activecluster.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.dindex.Partition;
import org.codehaus.wadi.dindex.PartitionConfig;
import org.codehaus.wadi.dindex.PartitionManager;
import org.codehaus.wadi.dindex.PartitionManagerConfig;
import org.codehaus.wadi.dindex.messages.PartitionEvacuationRequest;
import org.codehaus.wadi.dindex.messages.PartitionEvacuationResponse;
import org.codehaus.wadi.dindex.messages.PartitionRepopulateRequest;
import org.codehaus.wadi.dindex.messages.PartitionRepopulateResponse;
import org.codehaus.wadi.dindex.messages.PartitionTransferAcknowledgement;
import org.codehaus.wadi.dindex.messages.PartitionTransferCommand;
import org.codehaus.wadi.dindex.messages.PartitionTransferRequest;
import org.codehaus.wadi.dindex.messages.PartitionTransferResponse;
import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.gridstate.LockManager;
import org.codehaus.wadi.gridstate.PartitionMapper;
import org.codehaus.wadi.gridstate.impl.StupidLockManager;
import org.codehaus.wadi.impl.Quipu;

/**
 * A Simple PartitionManager.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 *
 */
public class SimplePartitionManager implements PartitionManager, PartitionConfig {

	public interface Callback {void onNodeRemoved(ClusterEvent event);}

	protected final static String _partitionKeysKey="partitionKeys";
	protected final static String _timeStampKey="timeStamp";
	protected final static String _correlationIDMapKey="correlationIDMap";

	protected final String _nodeName;
	protected final Log _log;
	protected final int _numPartitions;
	protected final PartitionFacade[] _partitions;
	protected final Cluster _cluster;
	protected final Dispatcher _dispatcher;
	protected final Map _distributedState;
	protected final long _inactiveTime;
	protected final boolean _allowRegenerationOfMissingPartitions = true;
	protected final Callback _callback;
	protected final PartitionMapper _mapper;
	protected final LockManager _pmSyncs;

	public SimplePartitionManager(Dispatcher dispatcher, int numPartitions, Map distributedState, Callback callback, PartitionMapper mapper) {
		_dispatcher=dispatcher;
		_nodeName=_dispatcher.getNodeName();
		_pmSyncs=new StupidLockManager(_nodeName);
		_log=LogFactory.getLog(getClass().getName()+"#"+_nodeName);
		_numPartitions=numPartitions;

		_partitions=new PartitionFacade[_numPartitions];
		long timeStamp=System.currentTimeMillis();
		boolean queueing=true;
		for (int i=0; i<_numPartitions; i++)
			_partitions[i]=new PartitionFacade(i, timeStamp, new DummyPartition(i), queueing, this);

		_cluster=_dispatcher.getCluster();
		_distributedState=distributedState;
		_inactiveTime=_dispatcher.getInactiveTime();
		_callback=callback;
		_mapper=mapper;
	}

	protected PartitionManagerConfig _config;

	public void init(PartitionManagerConfig config) {
		_config=config;
		_log.trace("init");
		// attach relevant message handlers to dispatcher...
		_dispatcher.register(this, "onPartitionTransferCommand", PartitionTransferCommand.class);
		_dispatcher.register(PartitionTransferAcknowledgement.class, _inactiveTime);
		_dispatcher.register(this, "onPartitionTransferRequest", PartitionTransferRequest.class);
		_dispatcher.register(PartitionTransferResponse.class, _inactiveTime);
		_dispatcher.register(this, "onPartitionEvacuationRequest", PartitionEvacuationRequest.class);
		_dispatcher.register(PartitionEvacuationResponse.class, _inactiveTime);
		_dispatcher.register(this, "onPartitionRepopulateRequest", PartitionRepopulateRequest.class);
		_dispatcher.register(PartitionRepopulateResponse.class, _inactiveTime);
	}

	public void start() throws Exception {
		_log.trace("starting...");
		_log.trace("...started");
	}

	public void evacuate() throws Exception {
		_log.info("evacuating...");

		PartitionEvacuationRequest request=new PartitionEvacuationRequest();
		Node localNode=_cluster.getLocalNode();
		String correlationId=_cluster.getLocalNode().getName();
		if (_log.isTraceEnabled()) _log.trace("evacuating partitions...: "+_dispatcher.getNodeName(localNode.getDestination())+" -> "+_config.getCoordinatorNode().getState().get("nodeName"));
		while (_dispatcher.exchangeSend(localNode.getDestination(), _config.getCoordinatorNode().getDestination(), correlationId, request, _inactiveTime)==null) {
			if (_log.isWarnEnabled()) _log.warn("could not contact Coordinator - backing off for "+ _inactiveTime+" millis...");
			Thread.sleep(_config.getInactiveTime());
		}

		_log.info("...evacuated");
	}

	public void stop() throws Exception {
		_log.info("stopping...");
		// detach relevant message handlers from dispatcher...
		_dispatcher.deregister("onPartitionTransferCommand", PartitionTransferCommand.class, 5000);
		_dispatcher.deregister("onPartitionTransferRequest", PartitionTransferRequest.class, 5000);
		_dispatcher.deregister("onPartitionEvacuationRequest", PartitionEvacuationRequest.class, 5000);
		_dispatcher.deregister("onPartitionRepopulateRequest", PartitionRepopulateRequest.class, 5000);
		_log.info("...stopped");
	}

	public PartitionFacade getPartition(int partition) {
		return _partitions[partition];
	}

	// a node wants to shutdown...
	public void onPartitionEvacuationRequest(ObjectMessage om, PartitionEvacuationRequest request) {
		Node from;
		try {
			Destination destination=om.getJMSReplyTo();
			Node local=_cluster.getLocalNode();
			if (destination.equals(local.getDestination()))
				from=local;
			else
				from=(Node)_cluster.getNodes().get(destination);
		} catch (JMSException e) {
			_log.warn("could not read src node from message", e);
			from=null;
		}

		assert (from!=null);
		_callback.onNodeRemoved(new ClusterEvent(_cluster, from, ClusterEvent.REMOVE_NODE));
	}

	// a node wants to rebuild a lost partition
	public void onPartitionRepopulateRequest(ObjectMessage om, PartitionRepopulateRequest request) {
		int keys[]=request.getKeys();
		if (_log.isTraceEnabled()) _log.trace("PartitionRepopulateRequest ARRIVED: " + keys);
		Collection[] c=createResultSet(_numPartitions, keys);
		try {
			_log.trace("findRelevantSessionNames - starting");
			_config.findRelevantSessionNames(_numPartitions, c);
			_log.trace("findRelevantSessionNames - finished");
		} catch (Throwable t) {
			_log.warn("ERROR", t);
		}
		if (!_dispatcher.reply(om, new PartitionRepopulateResponse(c)))
			_log.warn("unexpected problem responding to partition repopulation request");
	}

	// receive a command to transfer IndexPartitions to another node
	// send them in a request, waiting for response
	// send an acknowledgement to Coordinator who sent original command
	public void onPartitionTransferCommand(ObjectMessage om, PartitionTransferCommand command) {
		PartitionTransfer[] transfers=command.getTransfers();
		for (int i=0; i<transfers.length; i++) {
			PartitionTransfer transfer=transfers[i];
			int amount=transfer.getAmount();
			Destination destination=transfer.getDestination();

			// acquire partitions for transfer...
			LocalPartition[] acquired=null;
			try {
				Collection c=new ArrayList();
				for (int j=0; j<_numPartitions && c.size()<amount; j++) {
					PartitionFacade facade=_partitions[j];
					if (facade.isLocal()) {
						Partition partition=facade.getContent();
						c.add(partition);
					}
				}
				acquired=(LocalPartition[])c.toArray(new LocalPartition[c.size()]);
				assert (amount==acquired.length);

				long timeStamp=System.currentTimeMillis();

				// build request...
				if (_log.isTraceEnabled()) _log.trace("local state (before giving): " + getPartitionKeys());
				PartitionTransferRequest request=new PartitionTransferRequest(timeStamp, acquired);
				// send it...
				ObjectMessage om3=_dispatcher.exchangeSend(_dispatcher.getLocalDestination(), destination, request, _inactiveTime);
				// process response...
				if (om3!=null && ((PartitionTransferResponse)om3.getObject()).getSuccess()) {
					for (int j=0; j<acquired.length; j++) {
						PartitionFacade facade=null;
						facade=_partitions[acquired[j].getKey()];
						facade.setContentRemote(timeStamp, _dispatcher, destination); // TODO - should we use a more recent ts ?
					}
					if (_log.isDebugEnabled()) _log.debug("released "+acquired.length+" partition[s] to "+_dispatcher.getNodeName(destination));
				} else {
					_log.warn("transfer unsuccessful");
				}
			} catch (Throwable t) {
				_log.warn("unexpected problem", t);
			}
		}
		try {
			PartitionKeys keys=getPartitionKeys();
			_distributedState.put(_partitionKeysKey, keys);
			_distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
			if (_log.isTraceEnabled()) _log.trace("local state (after giving): " + keys);
			String correlationID=_dispatcher.getOutgoingCorrelationId(om);
			if (_log.isTraceEnabled()) _log.trace("CORRELATIONID: " + correlationID);
			Map correlationIDMap=(Map)_distributedState.get(_correlationIDMapKey);
			Destination from=om.getJMSReplyTo();
			correlationIDMap.put(from, correlationID);
			_dispatcher.setDistributedState(_distributedState);
			if (_log.isTraceEnabled()) _log.trace("distributed state updated: " + _dispatcher.getDistributedState());
			correlateStateUpdate(_distributedState); // onStateUpdate() does not get called locally
			correlationIDMap.remove(from);
			// FIXME - RACE - between update of distributed state and ack - they should be one and the same thing...
			//_dispatcher.reply(om, new PartitionTransferAcknowledgement(true)); // what if failure - TODO
		} catch (Exception e) {
			_log.warn("could not acknowledge safe transfer to Coordinator", e);
		}
	}

	// receive a transfer of partitions
	public synchronized void onPartitionTransferRequest(ObjectMessage om, PartitionTransferRequest request) {
		long timeStamp=request.getTimeStamp();
		LocalPartition[] partitions=request.getPartitions();
		boolean success=false;
		// read incoming data into our own local model
		if (_log.isTraceEnabled()) _log.trace("local state (before receiving): " + getPartitionKeys());
		for (int i=0; i<partitions.length; i++) {
			LocalPartition partition=partitions[i];
			partition.init(this);
			PartitionFacade facade=getPartition(partition.getKey());
			facade.setContent(timeStamp, partition);
		}
		success=true;
		try {
			PartitionKeys keys=getPartitionKeys();
			_distributedState.put(_partitionKeysKey, keys);
			_distributedState.put(_timeStampKey, new Long(System.currentTimeMillis()));
			if (_log.isTraceEnabled()) _log.trace("local state (after receiving): " + keys);
			_dispatcher.setDistributedState(_distributedState);
			if (_log.isTraceEnabled()) _log.trace("distributed state updated: " + _dispatcher.getDistributedState());
		} catch (Exception e) {
			_log.error("could not update distributed state", e);
		}
		// acknowledge safe receipt to donor
		if (_dispatcher.reply(om, new PartitionTransferResponse(success))) {
			// unlock Partitions here... - TODO
			try {
				if (_log.isDebugEnabled()) _log.debug("acquired "+partitions.length+" partition[s] from "+_dispatcher.getNodeName(om.getJMSReplyTo()));
			} catch (JMSException e) {
				_log.warn("problem reading incoming message's source", e);
			}
		} else {
			_log.warn("problem acknowledging reciept of IndexPartitions - donor may have died");
			// chuck them... - TODO
		}
	}

	// TODO - duplicate code (from DIndex)
	public Collection[] createResultSet(int numPartitions, int[] keys) {
		Collection[] c=new Collection[numPartitions];
		for (int i=0; i<keys.length; i++)
			c[keys[i]]=new ArrayList();
		return c;
	}

	// ClusterListener

	public void update(Node node) {
		Map state=node.getState();
		long timeStamp=((Long)state.get(_timeStampKey)).longValue();
		PartitionKeys keys=(PartitionKeys)state.get(_partitionKeysKey);
		Destination location=node.getDestination();
		int[] k=keys._keys;
		for (int i=0; i<k.length; i++) {
			int key=k[i];
			PartitionFacade facade=_partitions[key];
			facade.setContentRemote(timeStamp, _dispatcher, location);
		}
	}


	public void markExistingPartitions(Node[] nodes, boolean[] partitionIsPresent) {
		for (int i=0; i<nodes.length; i++) {
			Node node=nodes[i];
			if (node!=null) {
				PartitionKeys keys=DIndex.getPartitionKeys(node);
				if (keys!=null) {
					int[] k=keys.getKeys();
					for (int j=0; j<k.length; j++) {
						int index=k[j];
						if (partitionIsPresent[index]) {
							if (_log.isErrorEnabled()) _log.error("partition " + index + " found on more than one node");
						} else {
							partitionIsPresent[index]=true;
						}
					}
				}
			}
		}
	}

	public void regenerateMissingPartitions(Node[] living, Node[] leaving) {
		boolean[] partitionIsPresent=new boolean[_numPartitions];
		markExistingPartitions(living, partitionIsPresent);
		markExistingPartitions(leaving, partitionIsPresent);
		Collection missingPartitions=new ArrayList();
		for (int i=0; i<partitionIsPresent.length; i++) {
			if (!partitionIsPresent[i])
				missingPartitions.add(new Integer(i));
		}

		int numKeys=missingPartitions.size();
		if (numKeys>0) {
		  assert (_allowRegenerationOfMissingPartitions);
			// convert to int[]
			int[] missingKeys=new int[numKeys];
			int key=0;
			for (Iterator i=missingPartitions.iterator(); i.hasNext(); )
				missingKeys[key++]=((Integer)i.next()).intValue();

			if (_log.isWarnEnabled()) _log.warn("RECREATING PARTITIONS...: " + missingPartitions);
			long time=System.currentTimeMillis();
			for (int i=0; i<missingKeys.length; i++) {
				int k=missingKeys[i];
				PartitionFacade facade=_partitions[k];
				LocalPartition local=new LocalPartition(k);
				local.init(this);
				facade.setContent(time, local);
			}
			PartitionKeys newKeys=getPartitionKeys();
			if (_log.isWarnEnabled()) _log.warn("REPOPULATING PARTITIONS...: " + missingPartitions);
			String correlationId=_dispatcher.nextCorrelationId();
			Quipu rv=_dispatcher.setRendezVous(correlationId, _dispatcher.getNumNodes()-1);
			if (!_dispatcher.send(_dispatcher.getLocalDestination(), _dispatcher.getClusterDestination(), correlationId, new PartitionRepopulateRequest(missingKeys))) {
				_log.error("unexpected problem repopulating lost index");
			}

			// whilst we are waiting for the other nodes to get back to us, figure out which relevant sessions
			// we are carrying ourselves...
			Collection[] c=createResultSet(_numPartitions, missingKeys);
			_config.findRelevantSessionNames(_numPartitions, c);
			repopulate(_dispatcher.getLocalDestination(), c);

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
					PartitionRepopulateResponse response=(PartitionRepopulateResponse)message.getObject();
					Collection[] relevantKeys=response.getKeys();

					repopulate(from, relevantKeys);

				} catch (JMSException e) {
					_log.warn("unexpected problem interrogating response", e);
				}
			}

			if (_log.isWarnEnabled()) _log.warn("...PARTITIONS REPOPULATED: " + missingPartitions);
			// relayout dindex
			_distributedState.put(_partitionKeysKey, newKeys);
			try {
				_dispatcher.setDistributedState(_distributedState);
				if (_log.isTraceEnabled()) _log.trace("distributed state updated: " + _dispatcher.getDistributedState());
			} catch (Exception e) {
				_log.error("could not update distributed state", e);
			}
		}
	}

	public PartitionKeys getPartitionKeys() {
		return new PartitionKeys(_partitions);
	}

	public void repopulate(Destination location, Collection[] keys) {
	  assert (location!=null);
		for (int i=0; i<_numPartitions; i++) {
			Collection c=keys[i];
			if (c!=null) {
				PartitionFacade facade=_partitions[i];
				LocalPartition local=(LocalPartition)facade.getContent();
				for (Iterator j=c.iterator(); j.hasNext(); ) {
					String name=(String)j.next();
					local.put(name, location);
				}
			}
		}
	}

	public void localise() {
		if (_log.isDebugEnabled()) _log.debug("allocating " + _numPartitions + " partitions");
		long timeStamp=System.currentTimeMillis();
		for (int i=0; i<_numPartitions; i++) {
			PartitionFacade facade=_partitions[i];
			LocalPartition partition=new LocalPartition(i);
			partition.init(this);
			facade.setContent(timeStamp, partition);
		}
	}

	// TODO - duplicate code - see DIndex...
	protected void correlateStateUpdate(Map state) {
		Map correlationIDMap=(Map)state.get(_correlationIDMapKey);
		Destination local=_dispatcher.getLocalDestination();
		String correlationID=(String)correlationIDMap.get(local);
		if (correlationID!=null) {
			Quipu rv=(Quipu)_dispatcher.getRendezVousMap().get(correlationID);
			if (rv==null) {
				if (_log.isWarnEnabled()) _log.warn("no one waiting for: " + correlationID);
			} else {
				if (_log.isTraceEnabled()) _log.trace("successful correlation: " + correlationID);
				rv.putResult(state);
			}
		}
	}

//	public void repopulatePartitions(Destination location, Collection[] keys) {
//	for (int i=0; i<keys.length; i++) {
//	Collection c=keys[i];
//	if (c!=null) {
//	for (Iterator j=c.iterator(); j.hasNext(); ) {
//	String key=(String)j.next();
//	LocalPartition partition=(LocalPartition)_partitions[i].getContent();
//	partition._map.put(key, location);
//	}
//	}
//	}
//	}

	public int getNumPartitions() {
		return _numPartitions;
	}

	public PartitionFacade getPartition(Object key) {
		return _partitions[_mapper.map(key)];
	}

	// PartitionConfig API

	public Dispatcher getDispatcher() {
		return _dispatcher;
	}

	public Cluster getCluster() {
		return _cluster;
	}

	public String getNodeName(Destination destination) {
		return _dispatcher.getNodeName(destination);
	}

	public long getInactiveTime() {
		return _inactiveTime;
	}

	// PartitionConfig API

	public String getLocalNodeName() {
		return _nodeName;
	}

	public LockManager getPMSyncs() {
		return _pmSyncs;
	}

}
