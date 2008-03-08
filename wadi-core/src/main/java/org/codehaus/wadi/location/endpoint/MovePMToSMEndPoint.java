/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.location.endpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.core.MotableBusyException;
import org.codehaus.wadi.core.contextualiser.BasicInvocation;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.location.session.MovePMToSM;
import org.codehaus.wadi.location.session.MoveSMToIM;
import org.codehaus.wadi.location.session.MoveSMToPM;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpace;


/**
 * 
 * @version $Revision: 1538 $
 */
public class MovePMToSMEndPoint implements Lifecycle, MovePMToSMEndPointMessageListener {
    public static final ServiceName NAME = new ServiceName("MovePMToSMEndPoint");
    
    private static final Log log = LogFactory.getLog(MovePMToSMEndPoint.class);
    
    private final Dispatcher dispatcher;
    private final Contextualiser contextualiser;
    private final long sessionRelocationIMToSMAckWaitTime;
    private final ServiceEndpointBuilder endpointBuilder;
    private final ReplicationManager replicationManager;

    public MovePMToSMEndPoint(ServiceSpace serviceSpace,
            Contextualiser contextualiser,
            ReplicationManager replicationManager,
            long sessionRelocationIMToSMAckWaitTime) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == contextualiser) {
            throw new IllegalArgumentException("contextualiser is required");
        } else if (null == replicationManager) {
            throw new IllegalArgumentException("replicationManager is required");
        }
        this.contextualiser = contextualiser;
        this.replicationManager = replicationManager;
        this.sessionRelocationIMToSMAckWaitTime = sessionRelocationIMToSMAckWaitTime;

        dispatcher = serviceSpace.getDispatcher();
        endpointBuilder = new ServiceEndpointBuilder();
    }

    public void start() throws Exception {
        endpointBuilder.addSEI(dispatcher, MovePMToSMEndPointMessageListener.class, this);
    }

    public void stop() throws Exception {
        endpointBuilder.dispose(10, 500);
    }

    public void onMessage(Envelope message, MovePMToSM request) {
        boolean successfulRelocation = false;
        boolean sessionBuzy = false;
        Object key = request.getKey();
        try {
            RelocationImmoter promoter = new RelocationImmoter(dispatcher,
                    request,
                    replicationManager,
                    sessionRelocationIMToSMAckWaitTime);
            
            Invocation invocation = new BasicInvocation((String) key, request.getExclusiveSessionLockWaitTime());
            // if we own session, this will send the correct response...
            contextualiser.contextualise(invocation, (String) key, promoter, true);
            successfulRelocation = promoter.isSuccessfulRelocation();
            if (!successfulRelocation && !promoter.isSessionFound()) {
                log.warn("Motable [" + key + "] has just been destroyed");
                // send on null state from StateMaster to InvocationMaster...
                replyToInvocationMaster(request, new MoveSMToIM());
            }
        } catch (MotableBusyException e) {
            sessionBuzy = true;
            log.warn("Motable buzy [" + key + "]");
            // send session buzy event from StateMaster to InvocationMaster...
            replyToInvocationMaster(request, new MoveSMToIM(true));
        } catch (Exception e) {
            log.warn("problem handling relocation request: " + key, e);
        } finally {
            // PartitionMaster ack.
            try {
                dispatcher.reply(message, new MoveSMToPM(successfulRelocation, sessionBuzy));
            } catch (MessageExchangeException e) {
                log.error("Cannot ack to StateMaster", e);
            }
        }
    }

    protected void replyToInvocationMaster(MovePMToSM request, MoveSMToIM body) {
        try {
            dispatcher.reply(dispatcher.getCluster().getLocalPeer().getAddress(),
                request.getIMPeer().getAddress(),
                request.getIMCorrelationId(),
                body);
        } catch (MessageExchangeException e) {
            log.error("Cannot reply to InvocationMaster", e);
        }
    }

}
