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
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.impl.StupidLockManager;
import org.codehaus.wadi.location.PartitionConfig;
import org.codehaus.wadi.location.PartitionManager;
import org.codehaus.wadi.location.PartitionManagerConfig;
import org.codehaus.wadi.location.partition.BasicPartitionRepopulateTask;
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

/**
 * A Simple PartitionManager.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 *
 */
public class SimplePartitionManager implements PartitionManager, PartitionConfig, PartitionManagerMessageListener {
	private static final int WAIT_DEFINED_PARTITION_MANAGER = 60 * 1000;
	private static final long PARTITION_UPDATE_WAIT_TIME = 5000;
	private static final long PARTITION_LOCALIZATION_WAIT_TIME = 5000;

    public interface Callback {
        void onPartitionEvacuationRequest(ClusterEvent event);

        void onPartitionManagerJoiningEvent(PartitionManagerJoiningEvent joiningEvent);
    }

    private final Object balancingUpdateLock = new Object();
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

    private final Object balancingInfoLock = new Object();
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
        _inactiveTime = _cluster.getInactiveTime();
        _callback = callback;
        _mapper = mapper;

        _endpointBuilder = new ServiceEndpointBuilder();
    }

    protected void initializePartitionFacades() {
        for (int i = 0; i < _numPartitions; i++) {
            _partitions[i] = new VersionAwarePartitionFacade(_dispatcher, 
                    new PartitionFacadeDelegate(i, _dispatcher),
                    PARTITION_UPDATE_WAIT_TIME);
        }
        balancingInfo = new UnknownPartitionBalancingInfo(_localPeer, _numPartitions);
    }

    public void init(PartitionManagerConfig config) {
        _config = config;
    }

    public void start() throws Exception {
        initializePartitionFacades();
        _endpointBuilder.addSEI(_dispatcher, PartitionManagerMessageListener.class, this);
        _endpointBuilder.addCallback(_dispatcher, PartitionTransferAcknowledgement.class);
        _endpointBuilder.addCallback(_dispatcher, PartitionTransferResponse.class);
        _endpointBuilder.addCallback(_dispatcher, PartitionEvacuationResponse.class);
        _endpointBuilder.addCallback(_dispatcher, PartitionRepopulateResponse.class);
        
        evacuationCompletionLatch = new Latch();
        notifyPartitionManagerJoining();
        waitUntilUseable(WAIT_DEFINED_PARTITION_MANAGER);
    }
    
    public void stop() throws Exception {
        _endpointBuilder.dispose(10, 500);
    }

    protected void notifyPartitionManagerJoining() throws MessageExchangeException {
        PartitionManagerJoiningEvent event = new PartitionManagerJoiningEvent(_localPeer);
        Peer coordPeer = _config.getCoordinator();
        _dispatcher.send(coordPeer.getAddress(), event);
    }

    protected void waitUntilUseable(long attemptPeriod) throws InterruptedException, PartitionManagerException {
        for (int i = 0; i < _partitions.length; i++) {
            boolean success = _partitions[i].waitForBoot(attemptPeriod);
            if (!success) {
                throw new PartitionManagerException("Partition [" + i + "] is unknown.");
            }
        }
    }

    public void evacuate() {
        _log.info("Evacuating partitions...");
        evacuatingPartitions = true;

        PartitionEvacuationRequest request = new PartitionEvacuationRequest();
        Peer localPeer = _localPeer;
        Peer coordPeer = _config.getCoordinator();
        if (_log.isTraceEnabled()) {
            _log.trace("evacuating partitions...: " + localPeer + " -> " + coordPeer);
        }

        int failures = 0;
        boolean success = false;
        boolean interruptFlag = false;
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
                    interruptFlag = true;
                }
            } else {
                success = true;
            }
        }
        evacuatingPartitions = false;

        if (interruptFlag) {
            Thread.currentThread().interrupt();
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
        Collection[] c = request.createPartitionIndexToSessionNames(_numPartitions);
        _config.findRelevantSessionNames(_mapper, c);
        try {
            _dispatcher.reply(om, new PartitionRepopulateResponse(_localPeer, c));
        } catch (MessageExchangeException e) {
            _log.warn("unexpected problem responding to partition repopulation request", e);
        }
    }

    public void onPartitionManagerJoiningEvent(Envelope om, PartitionManagerJoiningEvent joiningEvent) {
        _callback.onPartitionManagerJoiningEvent(joiningEvent);
    }

    public void onRetrieveBalancingInfoEvent(Envelope om, RetrieveBalancingInfoEvent infoEvent) {
        PartitionBalancingInfoState state;
        synchronized (balancingInfoLock) {
            state = new PartitionBalancingInfoState(evacuatingPartitions, balancingInfo);
        }
        try {
            _dispatcher.reply(om, state);
        } catch (MessageExchangeException e) {
            _log.error("Cannot reply with current partition balancing info", e);
        }
    }

    public void onPartitionBalancingInfoUpdate(Envelope om, PartitionBalancingInfoUpdate infoUpdate) {
        synchronized (balancingUpdateLock) {
            try {
                doOnPartitionBalancingInfoUpdate(om, infoUpdate);
            } catch (Exception e) {
                _log.error("See nested", e);
                throw new RuntimeException(e);
            }
        }
    }
    
    protected void doOnPartitionBalancingInfoUpdate(Envelope om, PartitionBalancingInfoUpdate infoUpdate) throws MessageExchangeException, PartitionBalancingException {
        PartitionBalancingInfo newBalancingInfo = infoUpdate.buildNewPartitionInfo(_localPeer);
        
        if (infoUpdate.isPartitionManagerAlone()) {
            // This case happens when this PartitionManager is shutting down and is the last one. In this scenario,
            // its partitions are not evacuated.
            if (infoUpdate.isPartitionEvacuationAck()) {
                evacuationCompletionLatch.release();
            } else {
                localise(newBalancingInfo);
            }
        } else {
            transferPartitions(newBalancingInfo);
            if (infoUpdate.isPartitionEvacuationAck()) {
                evacuationCompletionLatch.release();
            } else {
                PartitionInfo[] newPartitionInfos = newBalancingInfo.getPartitionInfos();
                if (infoUpdate.isRepopulationRequested()) {
                    repopulatePartition(newPartitionInfos);
                }
                updateUnchangedPartitionFacades(newPartitionInfos);
                redefineRemotePartitions(newPartitionInfos);
                waitForPartitionTranserOrTimeout(newPartitionInfos);
            }
        }
        
        synchronized (balancingInfoLock) {
            balancingInfo = newBalancingInfo;
        }
    }

    protected void transferPartitions(PartitionBalancingInfo newBalancingInfo) throws MessageExchangeException, PartitionBalancingException {
        PartitionInfo[] newPartitionInfos = newBalancingInfo.getPartitionInfos();
        Map peerToFacadeToTransfer = identifyTransfers(newPartitionInfos);
        for (Iterator iter = peerToFacadeToTransfer.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            Peer peer = (Peer) entry.getKey();
            List partitionInfos = (List) entry.getValue();
            transferPartitionToPeers(peer, partitionInfos);
        }
    }

    protected void repopulatePartition(PartitionInfo[] partitionInfos) throws MessageExchangeException, PartitionBalancingException {
        BasicPartitionRepopulateTask repopulateTask = new BasicPartitionRepopulateTask(_dispatcher, _inactiveTime);
        
        LocalPartition[] toBePopulated = new LocalPartition[partitionInfos.length];
        for (int i = 0; i < partitionInfos.length; i++) {
            int key = partitionInfos[i].getIndex();
            toBePopulated[i] = new LocalPartition(_dispatcher, key, _inactiveTime);
        }
        
        repopulateTask.repopulate(toBePopulated);

        for (int i = 0; i < partitionInfos.length; i++) {
            PartitionInfo partitionInfo = partitionInfos[i];
            PartitionFacade facade = _partitions[partitionInfo.getIndex()];
            facade.setContent(partitionInfo, toBePopulated[i]);
        }
    }

    protected void redefineRemotePartitions(PartitionInfo[] newPartitionInfos) {
        for (int i = 0; i < newPartitionInfos.length; i++) {
            PartitionInfo newPartitionInfo = newPartitionInfos[i];
            Peer newOwner = newPartitionInfo.getOwner();
            if (newOwner.equals(_localPeer)) {
                continue;
            }
            PartitionFacade facade = _partitions[i];
            if (facade.isLocal()) {
                continue;
            }
            facade.setContentRemote(newPartitionInfo, newOwner);
        }
    }
    
    protected void updateUnchangedPartitionFacades(PartitionInfo[] newPartitionInfos) {
        for (int i = 0; i < newPartitionInfos.length; i++) {
            PartitionInfo newPartitionInfo = newPartitionInfos[i];
            Peer newOwner = newPartitionInfo.getOwner();
            if (!newOwner.equals(_localPeer)) {
                continue;
            }
            PartitionFacade facade = _partitions[i];
            if (!facade.isLocal()) {
                continue;
            }
            facade.setPartitionInfo(newPartitionInfo);
        }
    }

    protected void waitForPartitionTranserOrTimeout(PartitionInfo[] newPartitionInfos) throws MessageExchangeException, PartitionBalancingException {
        for (int i = 0; i < newPartitionInfos.length; i++) {
            PartitionInfo newPartitionInfo = newPartitionInfos[i];
            Peer newOwner = newPartitionInfo.getOwner();
            if (!newOwner.equals(_localPeer)) {
                continue;
            }
            PartitionFacade facade = _partitions[i];
            if (facade.isLocal()) {
                continue;
            }
            try {
                boolean success = facade.waitForLocalization(newPartitionInfo, PARTITION_LOCALIZATION_WAIT_TIME);
                if (!success) {
                    repopulatePartition(new PartitionInfo[] {newPartitionInfo});
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new PartitionBalancingException(e);
            }
        }
    }

    protected void transferPartitionToPeers(Peer peer, List partitionInfos) throws MessageExchangeException, PartitionBalancingException {
        Map partitionInfoToLocalPartition = new HashMap();
        Envelope responseEnv;
        try {
            for (Iterator iter = partitionInfos.iterator(); iter.hasNext();) {
                PartitionInfo partitionInfo = (PartitionInfo) iter.next();
                PartitionFacade facade = _partitions[partitionInfo.getIndex()];
                LocalPartition localPartition = (LocalPartition) facade.setContentRemote(partitionInfo, peer);
                partitionInfoToLocalPartition.put(partitionInfo, localPartition);
            }
            PartitionTransferRequest request = new PartitionTransferRequest(partitionInfoToLocalPartition);
            responseEnv = _dispatcher.exchangeSend(peer.getAddress(), request, _inactiveTime);
        } catch (MessageExchangeException e) {
            _log.error("Cannot transfer partitions to [" + peer + "]", e);
            throw e;
        }
        
        PartitionTransferResponse response = (PartitionTransferResponse) responseEnv.getPayload();
        if (response.isSuccess()) {
            for (Iterator iter = partitionInfoToLocalPartition.keySet().iterator(); iter.hasNext();) {
                PartitionInfo partitionInfo = (PartitionInfo) iter.next();
                PartitionFacade facade = _partitions[partitionInfo.getIndex()];
                facade.setContentRemote(partitionInfo, peer);
            }
            _log.info("Released " + partitionInfoToLocalPartition.size() + " partition[s] to " + peer);
        } else {
            _log.error("Cannot transfer partitions to [" + peer + "]");
            throw new PartitionBalancingException("Cannot transfer partitions to [" + peer + "]");
        }
    }

    protected Map identifyTransfers(PartitionInfo[] newPartitionInfos) {
        Map peerToPartitionInfos = new HashMap();
        for (int i = 0; i < _partitions.length; i++) {
            PartitionInfo newPartitionInfo = newPartitionInfos[i];
            Peer newOwner = newPartitionInfo.getOwner();
            if (newOwner.equals( _localPeer)) {
                continue;
            }
            PartitionFacade facade = _partitions[i];
            if (!facade.isLocal()) {
                continue;
            }
            List partitionInfos = (List) peerToPartitionInfos.get(newOwner);
            if (null == partitionInfos) {
                partitionInfos = new ArrayList();
                peerToPartitionInfos.put(newOwner, partitionInfos);
            }
            partitionInfos.add(newPartitionInfo);
        }
        return peerToPartitionInfos;
    }

    public void onPartitionTransferRequest(Envelope om, PartitionTransferRequest request) {
        Map partitionInfoToLocalPartition = request.getPartitionInfoToLocalPartition();
        for (Iterator iter = partitionInfoToLocalPartition.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            PartitionInfo partitionInfo = (PartitionInfo) entry.getKey();
            LocalPartition partition = (LocalPartition) entry.getValue();
            partition = new LocalPartition(_dispatcher, partition);
            PartitionFacade facade = _partitions[partitionInfo.getIndex()];
            facade.setContent(partitionInfo, partition);
        }

        // acknowledge safe receipt to donor
        try {
            _dispatcher.reply(om, new PartitionTransferResponse(true));
            _log.info("acquired " + partitionInfoToLocalPartition.size() + " partition[s] from "
                    + _dispatcher.getPeerName(om.getReplyTo()));
        } catch (MessageExchangeException e) {
            _log.error("problem acknowledging reciept of IndexPartitions - donor may have died", e);
        }
    }

    public PartitionBalancingInfo getBalancingInfo() {
        synchronized (balancingInfoLock) {
            return balancingInfo;
        }
    }
    
    protected void localise(PartitionBalancingInfo newBalancingInfo) {
        _log.info("Allocating " + _numPartitions + " partitions");
        PartitionInfo[] partitionInfos = newBalancingInfo.getPartitionInfos();
        for (int i = 0; i < _numPartitions; i++) {
            PartitionFacade facade = _partitions[i];
            if (facade.isLocal()) {
                facade.setPartitionInfo(partitionInfos[i]);
            } else {
                LocalPartition local = new LocalPartition(_dispatcher, i, _inactiveTime);
                facade.setContent(partitionInfos[i], local);
            }
        }
    }

    public int getNumPartitions() {
        return _numPartitions;
    }

    public PartitionFacade getPartition(Object key) {
        return _partitions[_mapper.map(key)];
    }

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

    public String getLocalPeerName() {
        return _nodeName;
    }
    
}
