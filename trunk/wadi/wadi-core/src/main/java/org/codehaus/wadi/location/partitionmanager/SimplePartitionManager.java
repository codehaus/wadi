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
package org.codehaus.wadi.location.partitionmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.location.balancing.PartitionBalancerSingletonService;
import org.codehaus.wadi.location.balancing.PartitionBalancerSingletonServiceHolder;
import org.codehaus.wadi.location.balancing.PartitionBalancingInfo;
import org.codehaus.wadi.location.balancing.PartitionBalancingInfoState;
import org.codehaus.wadi.location.balancing.PartitionBalancingInfoUpdate;
import org.codehaus.wadi.location.balancing.PartitionInfo;
import org.codehaus.wadi.location.balancing.PartitionInfoUpdate;
import org.codehaus.wadi.location.balancing.RetrieveBalancingInfoEvent;
import org.codehaus.wadi.location.partition.BasicPartitionRepopulateTask;
import org.codehaus.wadi.location.partition.PartitionEvacuationRequest;
import org.codehaus.wadi.location.partition.PartitionTransferRequest;
import org.codehaus.wadi.location.partitionmanager.facade.PartitionFacade;
import org.codehaus.wadi.location.partitionmanager.facade.PartitionFacadeDelegate;
import org.codehaus.wadi.location.partitionmanager.facade.VersionAwarePartitionFacade;
import org.codehaus.wadi.location.partitionmanager.local.LocalPartition;
import org.codehaus.wadi.location.statemanager.SimpleStateManager;
import org.codehaus.wadi.servicespace.InvocationMetaData;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * A Simple PartitionManager.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 */
public class SimplePartitionManager implements PartitionManager, PartitionManagerMessageListener {
    private static final Log log = LogFactory.getLog(SimpleStateManager.class);
    
    private final ServiceSpace serviceSpace;
    private final Dispatcher dispatcher;
    private final Peer localPeer;
    private final int numPartitions;
    private final PartitionMapper mapper;
    private final PartitionBalancerSingletonServiceHolder singletonServiceHolder;
    private final SimplePartitionManagerTiming timing;
    private final PartitionFacade[] partitions;
    private final ServiceEndpointBuilder endpointBuilder;
    private final Object balancingUnderExecution = new Object();
    private volatile boolean evacuatingPartitions;
    private Latch evacuationCompletionLatch;

    public SimplePartitionManager(ServiceSpace serviceSpace,
            int numPartitions,
            PartitionMapper mapper,
            PartitionBalancerSingletonServiceHolder singletonServiceHolder,
            SimplePartitionManagerTiming timing) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (1 > numPartitions) {
            throw new IllegalArgumentException("numPartitions must be > 0");
        } else if (null == mapper) {
            throw new IllegalArgumentException("mapper is required");
        } else if (null == singletonServiceHolder) {
            throw new IllegalArgumentException("singletonServiceHolder is required");
        } else if (null == timing) {
            throw new IllegalArgumentException("timing is required");
        }
        this.serviceSpace = serviceSpace;
        this.numPartitions = numPartitions;
        this.mapper = mapper;
        this.singletonServiceHolder = singletonServiceHolder;
        this.timing = timing;
        
        dispatcher = serviceSpace.getDispatcher();
        Cluster cluster = dispatcher.getCluster();
        localPeer = cluster.getLocalPeer();

        partitions = new PartitionFacade[numPartitions];
        
        endpointBuilder = new ServiceEndpointBuilder();
    }

    public void start() throws Exception {
        initializePartitionFacades();
        
        endpointBuilder.addSEI(dispatcher, PartitionManagerMessageListener.class, this);
        
        queueRebalancing();
        
        evacuationCompletionLatch = new Latch();
        waitForBoot();
    }

    public void stop() throws Exception {
        endpointBuilder.dispose(10, 500);
    }

    public void evacuate() {
        log.info("Evacuating partitions");
        evacuatingPartitions = true;

        PartitionEvacuationRequest request = new PartitionEvacuationRequest();
        int failures = 0;
        boolean success = false;
        boolean interruptFlag = false;
        while (!success && failures < 5) {
            // reinitialise in case coordinator has changed...
            Peer coordPeer = singletonServiceHolder.getHostingPeer();
            if (log.isTraceEnabled()) {
                log.trace("Evacuating partitions [" + localPeer + "] -> [" + coordPeer + "]");
            }
            boolean evacuationCompleted = false;
            try {
                dispatcher.send(coordPeer.getAddress(), request);
                evacuationCompleted = evacuationCompletionLatch.attempt(timing.getWaitForEvacuationTime());
            } catch (Exception e) {
                log.warn("Problem evacuating partitions", e);
            }
            if (!evacuationCompleted) {
                failures++;
                long evacuationBackoffTime = timing.getEvacuationBackoffTime();
                log.warn("Could not contact Coordinator - backing off for [" + evacuationBackoffTime + "] ms");
                try {
                    Thread.sleep(evacuationBackoffTime);
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
        
        log.info("Evacuated");
    }

    public void onPartitionEvacuationRequest(Envelope om, PartitionEvacuationRequest request) {
        singletonServiceHolder.getPartitionBalancerSingletonService().queueRebalancing();
    }

    public void onRetrieveBalancingInfoEvent(Envelope om, RetrieveBalancingInfoEvent infoEvent) {
        PartitionBalancingInfo balancingInfo;
        synchronized (balancingUnderExecution) {
            balancingInfo = buildBalancingInfo();
        }
        PartitionBalancingInfoState state = new PartitionBalancingInfoState(evacuatingPartitions, balancingInfo);
        try {
            dispatcher.reply(om, state);
        } catch (MessageExchangeException e) {
            log.warn("Cannot reply with current partition balancing info", e);
        }
    }

    public void onPartitionBalancingInfoUpdate(Envelope om, PartitionBalancingInfoUpdate infoUpdate) {
        synchronized (balancingUnderExecution) {
            try {
                doOnPartitionBalancingInfoUpdate(om, infoUpdate);
            } catch (Exception e) {
                log.error("See nested", e);
                throw new RuntimeException(e);
            } finally {
                if (infoUpdate.isPartitionEvacuationAck()) {
                    evacuationCompletionLatch.release();
                }
            }
        }
    }

    public void onPartitionTransferRequest(Envelope om, PartitionTransferRequest request) {
        Map partitionInfoToLocalPartition = request.getPartitionInfoToLocalPartition();
        for (Iterator iter = partitionInfoToLocalPartition.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            PartitionInfo partitionInfo = (PartitionInfo) entry.getKey();
            LocalPartition partition = (LocalPartition) entry.getValue();
            partition = new LocalPartition(dispatcher, partition);
            PartitionFacade facade = partitions[partitionInfo.getIndex()];
            facade.setContent(partitionInfo, partition);
        }
    }

    public PartitionBalancingInfo getBalancingInfo() {
        synchronized (balancingUnderExecution) {
            return buildBalancingInfo();
        }
    }
    
    public Partition getPartition(Object key) {
        return partitions[mapper.map(key)];
    }

    public ServiceSpace getServiceSpace() {
        return serviceSpace;
    }

    protected void queueRebalancing() {
        ServiceProxyFactory serviceProxyFactory = serviceSpace.getServiceProxyFactory(
                PartitionBalancerSingletonService.NAME,
                new Class[] { PartitionBalancerSingletonService.class });
        InvocationMetaData invocationMetaData = serviceProxyFactory.getInvocationMetaData();
        invocationMetaData.setOneWay(true);
        Peer hostingPeer = singletonServiceHolder.getHostingPeer();
        invocationMetaData.setTargets(new Peer[] {hostingPeer});
        PartitionBalancerSingletonService balancerSingletonService =
            (PartitionBalancerSingletonService) serviceProxyFactory .getProxy();
        balancerSingletonService.queueRebalancing();
    }
    
    protected void waitForBoot() throws InterruptedException, PartitionManagerException {
        for (int i = 0; i < partitions.length; i++) {
            boolean success = partitions[i].waitForBoot(timing.getWaitForBootTime());
            if (!success) {
                throw new PartitionManagerException("Partition [" + i + "] is unknown.");
            }
        }
    }

    protected void initializePartitionFacades() {
        for (int i = 0; i < numPartitions; i++) {
            partitions[i] = new VersionAwarePartitionFacade(dispatcher,
                    new PartitionInfo(0, i),
                    new PartitionFacadeDelegate(i, dispatcher),
                    timing.getWaitForPartitionUpdateTime());
        }
    }

    protected PartitionBalancingInfo buildBalancingInfo() {
        PartitionInfo[] partitionInfos = new PartitionInfo[partitions.length];
        for (int i = 0; i < partitions.length; i++) {
            PartitionFacade facade = partitions[i];
            PartitionInfo partitionInfo = facade.getPartitionInfo();
            partitionInfos[i] = partitionInfo;
        }
        return new PartitionBalancingInfo(localPeer, new PartitionBalancingInfo(partitionInfos));
    }
    
    protected void doOnPartitionBalancingInfoUpdate(Envelope om, PartitionBalancingInfoUpdate infoUpdate) throws MessageExchangeException, PartitionBalancingException {
        PartitionBalancingInfo newBalancingInfo = infoUpdate.buildNewPartitionInfo(localPeer);
        if (infoUpdate.isPartitionManagerAlone()) {
            // This case happens when this PartitionManager is shutting down and is the last one. In this scenario,
            // its partitions do not need to be localised.
            if (!infoUpdate.isPartitionEvacuationAck()) {
                localise(newBalancingInfo);
            }
        } else {
            PartitionInfo[] newPartitionInfos = newBalancingInfo.getPartitionInfos();
            transferPartitions(newPartitionInfos);
            repopulatePartition(newPartitionInfos, infoUpdate.getRepopulatePartitionInfoUpdates());
            redefineLocalUnchangedPartitions(newPartitionInfos);
            redefineRemotePartitions(newPartitionInfos);
        }
    }

    protected void transferPartitions(PartitionInfo[] newPartitionInfos) throws MessageExchangeException, PartitionBalancingException {
        Map peerToPartitionInfosToTransfer = identifyTransfers(newPartitionInfos);
        for (Iterator iter = peerToPartitionInfosToTransfer.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            Peer peer = (Peer) entry.getKey();
            List partitionInfos = (List) entry.getValue();
            transferPartitionToPeers(peer, partitionInfos);
        }
    }

    protected void repopulatePartition(PartitionInfo[] newPartitionInfos, PartitionInfoUpdate[] updates) throws MessageExchangeException, PartitionBalancingException {
        if (updates.length == 0) {
            return;
        }
        
        LocalPartition[] toBePopulated = new LocalPartition[updates.length];
        for (int i = 0; i < updates.length; i++) {
            int index = updates[i].getPartitionInfo().getIndex();
            toBePopulated[i] = new LocalPartition(dispatcher, index, timing.getSessionRelocationWaitTime());
        }
        
        BasicPartitionRepopulateTask repopulateTask =
            new BasicPartitionRepopulateTask(dispatcher, timing.getWaitForRepopulationTime());
        repopulateTask.repopulate(toBePopulated);

        for (int i = 0; i < updates.length; i++) {
            int index = updates[i].getPartitionInfo().getIndex();
            PartitionFacade facade = partitions[index];
            PartitionInfo partitionInfo = newPartitionInfos[index];
            facade.setContent(partitionInfo, toBePopulated[i]);
        }
    }

    protected void redefineRemotePartitions(PartitionInfo[] newPartitionInfos) {
        for (int i = 0; i < newPartitionInfos.length; i++) {
            PartitionInfo newPartitionInfo = newPartitionInfos[i];
            Peer newOwner = newPartitionInfo.getOwner();
            if (newOwner.equals(localPeer)) {
                continue;
            }
            PartitionFacade facade = partitions[i];
            if (facade.isLocal()) {
                continue;
            }
            facade.setContentRemote(newPartitionInfo, newOwner);
        }
    }
    
    protected void redefineLocalUnchangedPartitions(PartitionInfo[] newPartitionInfos) {
        for (int i = 0; i < newPartitionInfos.length; i++) {
            PartitionInfo newPartitionInfo = newPartitionInfos[i];
            Peer newOwner = newPartitionInfo.getOwner();
            if (!newOwner.equals(localPeer)) {
                continue;
            }
            PartitionFacade facade = partitions[i];
            if (!facade.isLocal()) {
                continue;
            }
            facade.setPartitionInfo(newPartitionInfo);
        }
    }

    protected void transferPartitionToPeers(Peer peer, List partitionInfos) throws MessageExchangeException, PartitionBalancingException {
        Map partitionInfoToLocalPartition = new HashMap();
        try {
            for (Iterator iter = partitionInfos.iterator(); iter.hasNext();) {
                PartitionInfo partitionInfo = (PartitionInfo) iter.next();
                PartitionFacade facade = partitions[partitionInfo.getIndex()];
                LocalPartition localPartition = (LocalPartition) facade.setContentRemote(partitionInfo, peer);
                localPartition.waitForClientCompletion();
                partitionInfoToLocalPartition.put(partitionInfo, localPartition);
            }
            PartitionTransferRequest request = new PartitionTransferRequest(partitionInfoToLocalPartition);
            dispatcher.send(peer.getAddress(), request);
        } catch (MessageExchangeException e) {
            log.error("Cannot transfer partitions to [" + peer + "]", e);
            throw e;
        }
    }

    protected Map identifyTransfers(PartitionInfo[] newPartitionInfos) {
        Map peerToPartitionInfos = new HashMap();
        for (int i = 0; i < partitions.length; i++) {
            PartitionInfo newPartitionInfo = newPartitionInfos[i];
            Peer newOwner = newPartitionInfo.getOwner();
            if (newOwner.equals(localPeer)) {
                continue;
            }
            PartitionFacade facade = partitions[i];
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

    protected void localise(PartitionBalancingInfo newBalancingInfo) {
        log.info("Allocating [" + numPartitions + "] partitions");
        PartitionInfo[] partitionInfos = newBalancingInfo.getPartitionInfos();
        for (int i = 0; i < numPartitions; i++) {
            PartitionFacade facade = partitions[i];
            if (facade.isLocal()) {
                facade.setPartitionInfo(partitionInfos[i]);
            } else {
                LocalPartition local = new LocalPartition(dispatcher, i, timing.getSessionRelocationWaitTime());
                facade.setContent(partitionInfos[i], local);
            }
        }
    }

}
