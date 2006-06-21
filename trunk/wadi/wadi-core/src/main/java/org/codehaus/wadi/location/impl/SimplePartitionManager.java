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
package org.codehaus.wadi.location.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.LockManager;
import org.codehaus.wadi.PartitionMapper;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.Quipu;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.impl.StupidLockManager;
import org.codehaus.wadi.location.Partition;
import org.codehaus.wadi.location.PartitionConfig;
import org.codehaus.wadi.location.PartitionManager;
import org.codehaus.wadi.location.PartitionManagerConfig;
import org.codehaus.wadi.location.messages.PartitionEvacuationRequest;
import org.codehaus.wadi.location.messages.PartitionEvacuationResponse;
import org.codehaus.wadi.location.messages.PartitionRepopulateRequest;
import org.codehaus.wadi.location.messages.PartitionRepopulateResponse;
import org.codehaus.wadi.location.messages.PartitionTransferAcknowledgement;
import org.codehaus.wadi.location.messages.PartitionTransferCommand;
import org.codehaus.wadi.location.messages.PartitionTransferRequest;
import org.codehaus.wadi.location.messages.PartitionTransferResponse;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * A Simple PartitionManager.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 *
 */
public class SimplePartitionManager implements PartitionManager, PartitionConfig, PartitionManagerMessageListener {

	public interface Callback {void onPeerRemoved(ClusterEvent event);}

	protected final static String _partitionKeysKey="partitionKeys";
	protected final static String _timeStampKey="timeStamp";
	protected final static String _correlationIDMapKey="correlationIDMap";

	protected final String _nodeName;
	protected final Log _log;
	protected final int _numPartitions;
	protected final PartitionFacade[] _partitions;
    protected Set _localPartitionKeys;
	protected final Cluster _cluster;
    protected final Peer _localPeer;
	protected final Dispatcher _dispatcher;
	protected final Map _distributedState;
	protected final long _inactiveTime;
	protected final boolean _allowRegenerationOfMissingPartitions = true;
	protected final Callback _callback;
	protected final PartitionMapper _mapper;
	protected final LockManager _pmSyncs;

    private final ServiceEndpointBuilder _endpointBuilder;

	public SimplePartitionManager(Dispatcher dispatcher, int numPartitions, Map distributedState, Callback callback, PartitionMapper mapper) {
		_dispatcher=dispatcher;
        _cluster=_dispatcher.getCluster();
        _localPeer=_cluster.getLocalPeer();
		_nodeName=_localPeer.getName();
		_pmSyncs=new StupidLockManager(_nodeName);
		_log=LogFactory.getLog(getClass().getName()+"#"+_nodeName);
		_numPartitions=numPartitions;
		_partitions=new PartitionFacade[_numPartitions];
        _localPartitionKeys=new TreeSet();
		long timeStamp=System.currentTimeMillis();
		boolean queueing=true;
		for (int i=0; i<_numPartitions; i++)
			_partitions[i]=new PartitionFacade(i, timeStamp, new UnknownPartition(i), queueing, this);

		_distributedState=distributedState;
		_inactiveTime=_cluster.getInactiveTime();
		_callback=callback;
		_mapper=mapper;

        _endpointBuilder = new ServiceEndpointBuilder();
	}

	protected PartitionManagerConfig _config;

	public void init(PartitionManagerConfig config) {
		_config=config;
		_log.trace("init");

		// attach relevant message handlers to dispatcher...
        _endpointBuilder.addSEI(_dispatcher, PartitionManagerMessageListener.class, this);
        _endpointBuilder.addCallback(_dispatcher, PartitionTransferAcknowledgement.class);
        _endpointBuilder.addCallback(_dispatcher, PartitionTransferResponse.class);
        _endpointBuilder.addCallback(_dispatcher, PartitionEvacuationResponse.class);
        _endpointBuilder.addCallback(_dispatcher, PartitionRepopulateResponse.class);
	}

	public void start() throws Exception {
		_log.trace("starting...");
		_log.trace("...started");
	}

	public void waitUntilUseable() throws InterruptedException {
		// wait until we are sure that all partitions Useable...
		for (int i=0; i<_partitions.length; i++) {
			Sync sync=_partitions[i]._lock.readLock();
			sync.acquire();
			sync.release();
		}
	}

	public void evacuate() {
	    _log.info("evacuating...");

	    PartitionEvacuationRequest request=new PartitionEvacuationRequest();
	    Peer localPeer=_localPeer;
        Peer coordPeer=_config.getCoordinator();
	    String correlationId=_localPeer.getName();
	    if (_log.isTraceEnabled()) _log.trace("evacuating partitions...: "+_dispatcher.getPeerName(localPeer.getAddress())+" -> "+coordPeer.getState().get(Peer._peerNameKey));

	    int failures=0;
	    boolean success=false;
	    while (!success && failures<5) {
	        coordPeer=_config.getCoordinator(); // reinitialise in case coordinator has changed...
		Message response=null;
	        try {
	            response=_dispatcher.exchangeSend(coordPeer.getAddress(), correlationId, request, _inactiveTime);
	        } catch (Exception e) {
	            _log.warn("problem evacuating partitions", e);
	        }
		if (response==null) {
		  failures++;
		  if (_log.isWarnEnabled()) _log.warn("could not contact Coordinator - backing off for "+ _inactiveTime+" millis...");
		  try {
		    Thread.sleep(_config.getInactiveTime());
		  } catch (InterruptedException e) {
		    // ignore;
		  }
		} else {
		  success=true;
		}
	    }

	    _log.info("...evacuated");
	}

	public void stop() throws Exception {
		_log.info("stopping...");
        // detach relevant message handlers from dispatcher...
        _endpointBuilder.dispose(10, 500);
        _log.info("...stopped");
	}

	public PartitionFacade getPartition(int partition) {
		return _partitions[partition];
	}

	// a node wants to shutdown...
	public void onPartitionEvacuationRequest(Message om, PartitionEvacuationRequest request) {
		Peer from;
        Address address=om.getReplyTo();
        Peer local=_localPeer;
        if (address.equals(local.getAddress())) {
            from=local;
        } else {
            from=(Peer)_cluster.getRemotePeers().get(address);
        }

		assert (from!=null);
		_callback.onPeerRemoved(new ClusterEvent(_cluster, from, ClusterEvent.PEER_REMOVED));
	}

	// a node wants to rebuild a lost partition
	public void onPartitionRepopulateRequest(Message om, PartitionRepopulateRequest request) {
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
        try {
            _dispatcher.reply(om, new PartitionRepopulateResponse(c));
        } catch (MessageExchangeException e) {
            _log.warn("unexpected problem responding to partition repopulation request", e);
        }
	}

    // temporary hack - ultimately we will keep track of local keys as they are added and removed from Partition array...
    private Set getLocalPartitionKeys() {
        Set keys=new TreeSet();
        for (int j=0; j<_numPartitions; j++) {
            PartitionFacade facade=_partitions[j];
            if (facade.isLocal()) {
                keys.add(new Integer(j));
            }
        }
        return keys;
    }
    
	// receive a command to transfer IndexPartitions to other Peers
	// send them in a request, waiting for response
	// send an acknowledgement to Coordinator who sent original command
    public synchronized void onPartitionTransferCommand(Message om, PartitionTransferCommand command) {
        if (_log.isTraceEnabled()) _log.trace("received: "+command);
        PartitionTransfer[] transfers=command.getTransfers();
        long timeStamp=System.currentTimeMillis();
        for (int i=0; i<transfers.length; i++) {
            PartitionTransfer transfer=transfers[i];
            if (_log.isTraceEnabled()) _log.trace("starting Partition transfer: "+transfer);
            int amount=transfer.getAmount();
            Address target=transfer.getAddress();

            // acquire partitions for transfer...
            LocalPartition[] acquired=null;
            Message m=null;
            try {
                Collection c=new ArrayList();

                _localPartitionKeys=getLocalPartitionKeys(); // TODO

                for (Iterator ii=_localPartitionKeys.iterator(); ii.hasNext() && c.size()<amount; ) {
                    int index=((Integer)ii.next()).intValue();
                    PartitionFacade facade=_partitions[index];
                    Partition partition=facade.acquire(); // acquire exclusive lock around partition
                    c.add(partition);
                }
                acquired=(LocalPartition[])c.toArray(new LocalPartition[c.size()]);
                assert (amount==acquired.length);


                // build request...
                if (_log.isTraceEnabled()) _log.trace("local state (before giving): " + getPartitionKeys());
                PartitionTransferRequest request=new PartitionTransferRequest(timeStamp, acquired);
                // send it...
                m=_dispatcher.exchangeSend(target, request, _inactiveTime);
            } catch (Throwable t) {
                _log.warn("unexpected problem", t);
            } finally {
                // process response...
                if (m!=null && ((PartitionTransferResponse)m.getPayload()).getSuccess()) {
                    for (int j=0; j<acquired.length; j++) {
                        PartitionFacade facade=_partitions[acquired[j].getKey()];
                        facade.release(target, timeStamp); // release exclusive lock resetting timeStamp and updating location
                    }
                    if (_log.isDebugEnabled()) _log.debug("released "+acquired.length+" partition[s] to "+_dispatcher.getPeerName(target));
                } else {
                    for (int j=0; j<acquired.length; j++) {
                        PartitionFacade facade=_partitions[acquired[j].getKey()];
                        facade.release(); // release exclusive lock
                    }
                    _log.warn("transfer unsuccessful");
                }
            }
        }
        
		try {
			PartitionKeys keys=getPartitionKeys();
			_distributedState.put(_partitionKeysKey, keys);
			_distributedState.put(_timeStampKey, new Long(timeStamp));
			if (_log.isTraceEnabled()) _log.trace("local state (after giving): " + keys);
			String correlationID=om.getSourceCorrelationId();
			if (_log.isTraceEnabled()) _log.trace("CORRELATIONID: " + correlationID);
			Map correlationIDMap=(Map)_distributedState.get(_correlationIDMapKey);
			Address from=om.getReplyTo();
			correlationIDMap.put(from, correlationID);
			_dispatcher.setDistributedState(_distributedState);
			if (_log.isTraceEnabled()) _log.trace("distributed state updated: " + _localPeer.getState());
			correlateStateUpdate(_distributedState); // onStateUpdate() does not get called locally
			correlationIDMap.remove(from);
			// FIXME - RACE - between update of distributed state and ack - they should be one and the same thing...
			//_dispatcher.reply(om, new PartitionTransferAcknowledgement(true)); // what if failure - TODO
		} catch (Exception e) {
			_log.warn("could not acknowledge safe transfer to Coordinator", e);
		}
	}

	// receive a transfer of partitions
	public synchronized void onPartitionTransferRequest(Message om, PartitionTransferRequest request) {
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
			if (_log.isTraceEnabled()) _log.trace("distributed state updated: " + _localPeer.getState());
		} catch (Exception e) {
			_log.error("could not update distributed state", e);
		}
		// acknowledge safe receipt to donor
        try {
            _dispatcher.reply(om, new PartitionTransferResponse(success));
            // unlock Partitions here... - TODO
            if (_log.isDebugEnabled()) _log.debug("acquired "+partitions.length+" partition[s] from "+_dispatcher.getPeerName(om.getReplyTo()));
        } catch (MessageExchangeException e) {
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

	public void update(Peer node) {

        Map state=node.getState();

        Long timeStampAsLong = (Long) state.get(_timeStampKey);
        if (null == timeStampAsLong) {
            return;
        }

		long timeStamp= timeStampAsLong.longValue();
		PartitionKeys keys=(PartitionKeys)state.get(_partitionKeysKey);
		Address location=node.getAddress();
		int[] k=keys._keys;
		for (int i=0; i<k.length; i++) {
			int key=k[i];
			PartitionFacade facade=_partitions[key];
			facade.setContentRemote(timeStamp, location);
		}
	}


	public void markExistingPartitions(Peer[] nodes, boolean[] partitionIsPresent) {
		for (int i=0; i<nodes.length; i++) {
			Peer node=nodes[i];
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

	public void regenerateMissingPartitions(Peer[] living, Peer[] leaving) {
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
			Quipu rv=_dispatcher.setRendezVous(correlationId, _dispatcher.getCluster().getPeerCount()-1);
            try {
                _dispatcher.send(_localPeer.getAddress(),
                                _dispatcher.getCluster().getAddress(),
                                correlationId,
                                new PartitionRepopulateRequest(missingKeys));
            } catch (MessageExchangeException e) {
                _log.error("unexpected problem repopulating lost index");
            }

			// whilst we are waiting for the other nodes to get back to us, figure out which relevant sessions
			// we are carrying ourselves...
			Collection[] c=createResultSet(_numPartitions, missingKeys);
			_config.findRelevantSessionNames(_numPartitions, c);
			repopulate(_localPeer.getAddress(), c);

			//boolean success=false;
			try {
				/*success=*/rv.waitFor(_inactiveTime);
			} catch (InterruptedException e) {
				_log.warn("unexpected interruption", e);
			}
			Collection results=rv.getResults();

			for (Iterator i=results.iterator(); i.hasNext(); ) {
				Message message=(Message)i.next();
                Address from=message.getReplyTo();
                PartitionRepopulateResponse response=(PartitionRepopulateResponse)message.getPayload();
                Collection[] relevantKeys=response.getKeys();

                repopulate(from, relevantKeys);
			}

			if (_log.isWarnEnabled()) _log.warn("...PARTITIONS REPOPULATED: " + missingPartitions);
			// relayout dindex
			_distributedState.put(_partitionKeysKey, newKeys);
			try {
				_dispatcher.setDistributedState(_distributedState);
				if (_log.isTraceEnabled()) _log.trace("distributed state updated: " + _localPeer.getState());
			} catch (Exception e) {
				_log.error("could not update distributed state", e);
			}
		}
	}

	public PartitionKeys getPartitionKeys() {
		return new PartitionKeys(_partitions);
	}

	public void repopulate(Address location, Collection[] keys) {
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
		Address local=_localPeer.getAddress();
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

//	public void repopulatePartitions(Address location, Collection[] keys) {
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

	public String getPeerName(Address address) {
		return _dispatcher.getPeerName(address);
	}

	public long getInactiveTime() {
		return _inactiveTime;
	}

	// PartitionConfig API

	public String getLocalPeerName() {
		return _nodeName;
	}

	public LockManager getPMSyncs() {
		return _pmSyncs;
	}

}
