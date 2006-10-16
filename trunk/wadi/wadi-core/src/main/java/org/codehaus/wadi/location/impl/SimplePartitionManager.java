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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.LockManager;
import org.codehaus.wadi.PartitionMapper;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterEvent;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.Quipu;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.impl.StupidLockManager;
import org.codehaus.wadi.location.PartitionConfig;
import org.codehaus.wadi.location.PartitionManager;
import org.codehaus.wadi.location.PartitionManagerConfig;
import org.codehaus.wadi.location.partition.PartitionEvacuationRequest;
import org.codehaus.wadi.location.partition.PartitionEvacuationResponse;
import org.codehaus.wadi.location.partition.PartitionRepopulateRequest;
import org.codehaus.wadi.location.partition.PartitionRepopulateResponse;
import org.codehaus.wadi.location.partition.PartitionTransferAcknowledgement;
import org.codehaus.wadi.location.partition.PartitionTransferRequest;
import org.codehaus.wadi.location.partition.PartitionTransferResponse;
import org.codehaus.wadi.partition.PartitionBalancingInfo;
import org.codehaus.wadi.partition.PartitionBalancingInfoState;
import org.codehaus.wadi.partition.PartitionBalancingInfoUpdate;
import org.codehaus.wadi.partition.PartitionInfo;
import org.codehaus.wadi.partition.RetrieveBalancingInfoEvent;
import org.codehaus.wadi.partition.UnknownPartitionBalancingInfo;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * A Simple PartitionManager.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 *
 */
public class SimplePartitionManager implements PartitionManager, PartitionConfig, PartitionManagerMessageListener {
	private static final int WAIT_DEFINED_PARTITION_MANAGER = 60 * 1000;

    public interface Callback {
        void onPartitionEvacuationRequest(ClusterEvent event);

        void onPartitionManagerJoiningEvent(PartitionManagerJoiningEvent joiningEvent);
    }

    protected final String _nodeName;
    protected final Log _log;
    protected final int _numPartitions;
    protected final PartitionFacade[] _partitions;
    protected final Cluster _cluster;
    protected final Peer _localPeer;
    protected final Dispatcher _dispatcher;
    protected final long _inactiveTime;
    protected final Callback _callback;
    protected final PartitionMapper _mapper;
    protected final LockManager _pmSyncs;
    private final ServiceEndpointBuilder _endpointBuilder;

    private PartitionBalancingInfo balancingInfo;
    private volatile boolean evacuatingPartitions;
    private Latch evacuationCompletionLatch;
    protected PartitionManagerConfig _config;

    public SimplePartitionManager(Dispatcher dispatcher, int numPartitions, Callback callback, PartitionMapper mapper) {
        _dispatcher = dispatcher;
        _cluster = _dispatcher.getCluster();
        _localPeer = _cluster.getLocalPeer();
        _nodeName = _localPeer.getName();
        _pmSyncs = new StupidLockManager(_nodeName);
        _log = LogFactory.getLog(getClass().getName() + "#" + _nodeName);
        _numPartitions = numPartitions;
        _partitions = new PartitionFacade[_numPartitions];
        for (int i = 0; i < _numPartitions; i++) {
            _partitions[i] = new PartitionFacade(i, new UnknownPartition(i), true, this);
        }
        balancingInfo = new UnknownPartitionBalancingInfo(_localPeer, numPartitions);

        _inactiveTime = _cluster.getInactiveTime();
        _callback = callback;
        _mapper = mapper;

        _endpointBuilder = new ServiceEndpointBuilder();
    }

    public void init(PartitionManagerConfig config) {
        _config = config;
    }

    public synchronized void start() throws Exception {
        _endpointBuilder.addSEI(_dispatcher, PartitionManagerMessageListener.class, this);
        _endpointBuilder.addCallback(_dispatcher, PartitionTransferAcknowledgement.class);
        _endpointBuilder.addCallback(_dispatcher, PartitionTransferResponse.class);
        _endpointBuilder.addCallback(_dispatcher, PartitionEvacuationResponse.class);
        _endpointBuilder.addCallback(_dispatcher, PartitionRepopulateResponse.class);
        
        evacuationCompletionLatch = new Latch();
        notifyPartitionManagerJoining();
        waitUntilUseable(WAIT_DEFINED_PARTITION_MANAGER);
    }
    
    public synchronized void stop() throws Exception {
        _endpointBuilder.dispose(10, 500);
    }

    protected void notifyPartitionManagerJoining() throws MessageExchangeException {
        PartitionManagerJoiningEvent event = new PartitionManagerJoiningEvent(_localPeer);
        Peer coordPeer = _config.getCoordinator();
        _dispatcher.send(coordPeer.getAddress(), event);
    }

    protected void waitUntilUseable(long attemptPeriod) throws InterruptedException, PartitionManagerException {
        for (int i = 0; i < _partitions.length; i++) {
            Sync sync = _partitions[i]._lock.writeLock();
            boolean success = sync.attempt(attemptPeriod);
            if (!success) {
                throw new PartitionManagerException("Partition [" + i + "] is unknown.");
            }
            sync.release();
        }
    }

    public void evacuate() {
        _log.info("Evacuating partitions...");
        evacuatingPartitions = true;

        PartitionEvacuationRequest request = new PartitionEvacuationRequest();
        Peer localPeer = _localPeer;
        Peer coordPeer = _config.getCoordinator();
        if (_log.isTraceEnabled()) {
            _log.trace("evacuating partitions...: " + localPeer.getName() + " -> " + coordPeer.getName());
        }

        int failures = 0;
        boolean success = false;
        while (!success && failures < 5) {
            // reinitialise in case coordinator has changed...
            coordPeer = _config.getCoordinator();
            boolean evacuationCompleted = false;
            try {
                _dispatcher.send(coordPeer.getAddress(), request);
                evacuationCompleted = evacuationCompletionLatch.attempt(_config.getInactiveTime());
            } catch (Exception e) {
                _log.warn("Problem evacuating partitions", e);
            }
            if (!evacuationCompleted) {
                failures++;
                _log.warn("Could not contact Coordinator - backing off for " + _inactiveTime + " millis...");
                try {
                    Thread.sleep(_config.getInactiveTime());
                } catch (InterruptedException e) {
                    // ignore;
                }
            } else {
                success = true;
            }
        }

        _log.info("...evacuated");
    }

    public PartitionFacade getPartition(int partition) {
        return _partitions[partition];
    }

    public void onPartitionEvacuationRequest(Envelope om, PartitionEvacuationRequest request) {
        Peer from;
        Address address = om.getReplyTo();
        Peer local = _localPeer;
        if (address.equals(local.getAddress())) {
            from = local;
        } else {
            from = (Peer) _cluster.getRemotePeers().get(address);
        }

        assert (from != null);
        _callback.onPartitionEvacuationRequest(new ClusterEvent(_cluster, from, ClusterEvent.PEER_REMOVED));
    }

    public void onPartitionRepopulateRequest(Envelope om, PartitionRepopulateRequest request) {
        int keys[] = request.getKeys();
        _log.info("PartitionRepopulateRequest ARRIVED: " + keys);
        Collection[] c = request.createPartitionIndexToSessionNames(_numPartitions);
        try {
            _config.findRelevantSessionNames(_numPartitions, c);
        } catch (Throwable t) {
            _log.warn("ERROR", t);
        }
        try {
            _dispatcher.reply(om, new PartitionRepopulateResponse(c));
        } catch (MessageExchangeException e) {
            _log.warn("unexpected problem responding to partition repopulation request", e);
        }
    }

    public void onPartitionManagerJoiningEvent(Envelope om, PartitionManagerJoiningEvent joiningEvent) {
        _callback.onPartitionManagerJoiningEvent(joiningEvent);
    }

    public void onRetrieveBalancingInfoEvent(Envelope om, RetrieveBalancingInfoEvent infoEvent) {
        try {
            _dispatcher.reply(om, new PartitionBalancingInfoState(evacuatingPartitions, balancingInfo));
        } catch (MessageExchangeException e) {
            _log.error("Cannot reply with current partition balancing info", e);
        }
    }

    /**
     * TODO lock partition manager as a whole. Properly handle exceptions.
     */
    public void onPartitionBalancingInfoUpdate(Envelope om, PartitionBalancingInfoUpdate infoUpdate) {
        balancingInfo = infoUpdate.buildNewPartitionInfo(_localPeer);
        
        if (infoUpdate.isPartitionManagerAlone()) {
            if (infoUpdate.isPartitionEvacuationAck()) {
                evacuationCompletionLatch.release();
            } else {
                localise();
            }
            return;
        }

        PartitionInfo[] newPartitionInfos = balancingInfo.getPartitionInfos();
        Map peerToFacadeToTransfer = identifyPartitionsToTransfers(newPartitionInfos);

        for (Iterator iter = peerToFacadeToTransfer.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            Peer peer = (Peer) entry.getKey();
            List facades = (List) entry.getValue();
            transferPartitionToPeers(peer, facades);
        }

        if (infoUpdate.isRepopulationRequested()) {
            int[] partitionIndicesToRepopulate = infoUpdate.getPartitionIndicesToRepopulate();
            repopulatePartition(partitionIndicesToRepopulate);
        }
        
        if (infoUpdate.isPartitionEvacuationAck()) {
            evacuationCompletionLatch.release();
        }

        redefineRemotePartitions(newPartitionInfos);
    }

    protected void repopulatePartition(int[] partitionIndicesToRepopulate) {
        _log.info("REPOPULATING PARTITIONS...: " + partitionIndicesToRepopulate);

        Quipu rv = _dispatcher.newRendezVous(_dispatcher.getCluster().getPeerCount() - 1);
        PartitionRepopulateRequest partitionRepopulateRequest = 
            new PartitionRepopulateRequest(partitionIndicesToRepopulate);
        try {
            _dispatcher.send(_localPeer.getAddress(), _dispatcher.getCluster().getAddress(), rv.getCorrelationId(),
                    partitionRepopulateRequest);
        } catch (MessageExchangeException e) {
            _log.error("unexpected problem repopulating lost index");
        }

        // whilst we are waiting for the other nodes to get back to us, figure out which relevant sessions 
        // we are carrying ourselves...
        Collection[] c = partitionRepopulateRequest.createPartitionIndexToSessionNames(_numPartitions);
        _config.findRelevantSessionNames(_numPartitions, c);
        repopulate(_localPeer.getAddress(), c);

        // boolean success=false;
        try {
            // TODO - I, Gianny, think this is incorrect.
            /* success= */rv.waitFor(_inactiveTime);
        } catch (InterruptedException e) {
            _log.warn("unexpected interruption", e);
        }
        
        Collection results = rv.getResults();
        for (Iterator i = results.iterator(); i.hasNext();) {
            Envelope message = (Envelope) i.next();
            Address from = message.getReplyTo();
            PartitionRepopulateResponse response = (PartitionRepopulateResponse) message.getPayload();
            Collection[] relevantKeys = response.getKeys();
            repopulate(from, relevantKeys);
        }

        _log.info("...PARTITIONS REPOPULATED: " + partitionIndicesToRepopulate);
    }

    protected void redefineRemotePartitions(PartitionInfo[] newPartitionInfos) {
        for (int i = 0; i < newPartitionInfos.length; i++) {
            PartitionFacade facade = _partitions[i];
            if (facade.isLocal()) {
                continue;
            }
            PartitionInfo newPartitionInfo = newPartitionInfos[i];
            Peer newOwner = newPartitionInfo.getOwner();
            if (newOwner.equals(_localPeer)) {
                continue;
            }
            if (facade.isUnknown()) {
                facade.setContentRemote(newOwner.getAddress());
            } else {
                facade.release(newOwner.getAddress());
            }
        }
    }

    protected void transferPartitionToPeers(Peer peer, List facades) {
        LocalPartition[] acquired = new LocalPartition[facades.size()];
        Envelope replyMsg = null;
        try {
            for (int i = 0; i < acquired.length; i++) {
                PartitionFacade facade = (PartitionFacade) facades.get(i);
                LocalPartition partition = (LocalPartition) facade.acquire();
                acquired[i] = partition;
            }
            PartitionTransferRequest request = new PartitionTransferRequest(acquired);
            replyMsg = _dispatcher.exchangeSend(peer.getAddress(), request, _inactiveTime);
        } catch (MessageExchangeException e) {
            throw (UnsupportedOperationException) new UnsupportedOperationException().initCause(e);
        } finally {
            if (null != replyMsg && ((PartitionTransferResponse) replyMsg.getPayload()).getSuccess()) {
                for (int i = 0; i < acquired.length; i++) {
                    PartitionFacade facade = _partitions[acquired[i].getKey()];
                    facade.release(peer.getAddress());
                }
                _log.info("Released " + acquired.length + " partition[s] to " + peer);
            } else {
                for (int i = 0; i < acquired.length; i++) {
                    if (null == acquired[i]) {
                        break;
                    }
                    PartitionFacade facade = _partitions[acquired[i].getKey()];
                    facade.release();
                }
                _log.warn("Transfer unsuccessful");
            }
        }
    }

    protected Map identifyPartitionsToTransfers(PartitionInfo[] newPartitionInfos) {
        Map peerToFacadeToTransfer = new HashMap();
        for (int i = 0; i < _partitions.length; i++) {
            PartitionFacade facade = _partitions[i];
            if (!facade.isLocal()) {
                continue;
            }
            PartitionInfo newPartitionInfo = newPartitionInfos[i];
            Peer newOwner = newPartitionInfo.getOwner();
            if (newOwner.equals(_localPeer)) {
                continue;
            }
            List facades = (List) peerToFacadeToTransfer.get(newOwner);
            if (null == facades) {
                facades = new ArrayList();
                peerToFacadeToTransfer.put(newOwner, facades);
            }
            facades.add(facade);
        }
        return peerToFacadeToTransfer;
    }

    public void onPartitionTransferRequest(Envelope om, PartitionTransferRequest request) {
        LocalPartition[] partitions = request.getPartitions();
        for (int i = 0; i < partitions.length; i++) {
            LocalPartition partition = partitions[i];
            partition.init(this);
            PartitionFacade facade = getPartition(partition.getKey());
            facade.setContent(partition);
        }

        // acknowledge safe receipt to donor
        try {
            _dispatcher.reply(om, new PartitionTransferResponse(true));
            // unlock Partitions here... - TODO
            if (_log.isDebugEnabled()) {
                _log.debug("acquired " + partitions.length + " partition[s] from "
                        + _dispatcher.getPeerName(om.getReplyTo()));
            }
        } catch (MessageExchangeException e) {
            _log.warn("problem acknowledging reciept of IndexPartitions - donor may have died");
            // chuck them... - TODO
        }
    }

    // ClusterListener
    public PartitionBalancingInfo getBalancingInfo() {
        return balancingInfo;
    }
    
    protected void repopulate(Address location, Collection[] partitionIndexToSessionNames) {
        for (int i = 0; i < _numPartitions; i++) {
            Collection sessionNames = partitionIndexToSessionNames[i];
            if (sessionNames != null) {
                PartitionFacade facade = _partitions[i];
                LocalPartition local = (LocalPartition) facade.getContent();
                for (Iterator j = sessionNames.iterator(); j.hasNext();) {
                    String name = (String) j.next();
                    local.put(name, location);
                }
            }
        }
    }

    protected void localise() {
        if (_log.isDebugEnabled()) {
            _log.debug("allocating " + _numPartitions + " partitions");
        }
        for (int i = 0; i < _numPartitions; i++) {
            PartitionFacade facade = _partitions[i];
            LocalPartition partition = new LocalPartition(i);
            partition.init(this);
            facade.setContent(partition);
        }
    }

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
}
