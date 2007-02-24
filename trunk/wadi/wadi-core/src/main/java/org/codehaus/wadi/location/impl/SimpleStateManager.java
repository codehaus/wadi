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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.location.Partition;
import org.codehaus.wadi.location.PartitionFacadeException;
import org.codehaus.wadi.location.PartitionManager;
import org.codehaus.wadi.location.StateManager;
import org.codehaus.wadi.location.session.DeleteIMToPM;
import org.codehaus.wadi.location.session.DeletePMToIM;
import org.codehaus.wadi.location.session.EvacuateIMToPM;
import org.codehaus.wadi.location.session.EvacuatePMToIM;
import org.codehaus.wadi.location.session.InsertIMToPM;
import org.codehaus.wadi.location.session.InsertPMToIM;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.MoveIMToSM;
import org.codehaus.wadi.location.session.MovePMToIM;
import org.codehaus.wadi.location.session.MovePMToIMInvocation;
import org.codehaus.wadi.location.session.MoveSMToIM;
import org.codehaus.wadi.location.session.MoveSMToPM;
import org.codehaus.wadi.location.session.ReleaseEntryRequest;
import org.codehaus.wadi.location.session.ReleaseEntryResponse;
import org.codehaus.wadi.servicespace.ServiceSpace;

public class SimpleStateManager implements StateManager, StateManagerMessageListener {
    private static final Log log = LogFactory.getLog(SimpleStateManager.class);
    
    private final Dispatcher dispatcher;
    private final PartitionManager partitionManager;
    private final LocalPeer localPeer;
    private final long inactiveTime;
    private final ServiceEndpointBuilder endpointBuilder;

    public SimpleStateManager(ServiceSpace serviceSpace,
            PartitionManager partitionManager,
            long inactiveTime) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == partitionManager) {
            throw new IllegalArgumentException("partitionManager is required");
        }
        this.dispatcher = serviceSpace.getDispatcher();
        this.partitionManager = partitionManager;
        this.inactiveTime = inactiveTime;

        localPeer = dispatcher.getCluster().getLocalPeer();
        endpointBuilder = new ServiceEndpointBuilder();
    }

    public void start() throws Exception {
        endpointBuilder.addSEI(dispatcher, StateManagerMessageListener.class, this);
        endpointBuilder.addCallback(dispatcher, InsertPMToIM.class);
        endpointBuilder.addCallback(dispatcher, DeletePMToIM.class);
        endpointBuilder.addCallback(dispatcher, EvacuatePMToIM.class);

        // GridState - Relocate - 5 messages - IM->PM->SM->IM->SM->PM
        endpointBuilder.addCallback(dispatcher, MoveSMToIM.class);
        endpointBuilder.addCallback(dispatcher, MoveIMToSM.class);
        endpointBuilder.addCallback(dispatcher, MoveSMToPM.class);
        // or possibly - IM->PM->IM (failure)
        endpointBuilder.addCallback(dispatcher, MovePMToIM.class);
        // or possibly - IM->PM->IM (Invocation relocation)
        endpointBuilder.addCallback(dispatcher, MovePMToIMInvocation.class);

        endpointBuilder.addCallback(dispatcher, ReleaseEntryResponse.class);
    }

    public void stop() throws Exception {
        endpointBuilder.dispose(10, 500);
    }

    public boolean insert(String name) {
        try {
            InsertIMToPM request = new InsertIMToPM(name, localPeer);
            Partition partition = partitionManager.getPartition(name);
            Envelope reply = partition.exchange(request, inactiveTime);
            return ((InsertPMToIM) reply.getPayload()).getSuccess();
        } catch (MessageExchangeException e) {
            log.error("See nested", e);
            return false;
        } catch (PartitionFacadeException e) {
            log.error("See nested", e);
            return false;
        }
    }

    public void remove(String name) {
        try {
            DeleteIMToPM request = new DeleteIMToPM(name);
            partitionManager.getPartition(name).exchange(request, inactiveTime);
        } catch (MessageExchangeException e) {
            log.error("See nested", e);
        } catch (PartitionFacadeException e) {
            log.error("See nested", e);
        }
    }

    public void relocate(String name) {
        try {
            EvacuateIMToPM request = new EvacuateIMToPM(name, localPeer);
            partitionManager.getPartition(name).exchange(request, inactiveTime);
        } catch (MessageExchangeException e) {
            log.info("See nested", e);
        } catch (PartitionFacadeException e) {
            log.error("See nested", e);
        }
    }

    public void onInsertIMToPM(Envelope om, InsertIMToPM request) {
        partitionManager.getPartition(request.getKey()).onMessage(om, request);
    }

    public void onDeleteIMToPM(Envelope om, DeleteIMToPM request) {
        partitionManager.getPartition(request.getKey()).onMessage(om, request);
    }

    public void onEvacuateIMToPM(Envelope om, EvacuateIMToPM request) {
        partitionManager.getPartition(request.getKey()).onMessage(om, request);
    }

    public void onMoveIMToPM(Envelope message, MoveIMToPM request) {
        partitionManager.getPartition(request.getKey()).onMessage(message, request);
    }

    public boolean offerEmigrant(Motable emotable) {
        String key = emotable.getName();
        Partition partition = partitionManager.getPartition(key);
        ReleaseEntryRequest pojo = new ReleaseEntryRequest(emotable);
        Envelope response = null;
        try {
            response = partition.exchange(pojo, inactiveTime);
            ReleaseEntryResponse releaseResponse = (ReleaseEntryResponse) response.getPayload();
            if (log.isTraceEnabled()) {
                log.trace("received acknowledgement (" + (releaseResponse.isSuccess() ? "good" : "bad")
                        + ") within timeframe (" + inactiveTime + " millis): " + key);
            }
            return releaseResponse.isSuccess();
        } catch (Exception e) {
            log.error("no acknowledgement within timeframe (" + inactiveTime + " millis): " + key, e);
            return false;
        }
    }

}
