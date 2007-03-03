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
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.manager.SessionMonitor;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.location.session.MovePMToSM;
import org.codehaus.wadi.location.session.MoveSMToIM;
import org.codehaus.wadi.location.session.MoveSMToPM;
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
    private final SessionMonitor sessionMonitor;
    private final long inactiveTime;
    private final ServiceEndpointBuilder endpointBuilder;


    public MovePMToSMEndPoint(ServiceSpace serviceSpace,
            Contextualiser contextualiser,
            SessionMonitor sessionMonitor,
            long inactiveTime) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == contextualiser) {
            throw new IllegalArgumentException("contextualiser is required");
        } else if (null == sessionMonitor) {
            throw new IllegalArgumentException("sessionMonitor is required");
        }
        this.contextualiser = contextualiser;
        this.sessionMonitor = sessionMonitor;
        this.inactiveTime = inactiveTime;

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
        Object key = request.getKey();
        try {
            Peer imPeer = request.getIMPeer();
            RelocationImmoter promoter = new RelocationImmoter(sessionMonitor,
                    dispatcher,
                    message,
                    request,
                    inactiveTime);
            // if we own session, this will send the correct response...
            contextualiser.contextualise(null, (String) key, promoter, true);
            if (!promoter.isFound()) {
                log.warn("state not found - perhaps it has just been destroyed: " + key);
                MoveSMToIM req = new MoveSMToIM(null);
                // send on null state from StateMaster to InvocationMaster...
                log.info("sending 0 bytes to : " + imPeer);
                Envelope ignore = dispatcher.exchangeSend(imPeer.getAddress(), req, inactiveTime, request
                        .getIMCorrelationId());
                log.info("received: " + ignore);
                // StateMaster replies to PartitionMaster indicating failure...
                log.info("reporting failure to PM");
                dispatcher.reply(message, new MoveSMToPM(false));
            }
        } catch (Exception e) {
            log.warn("problem handling relocation request: " + key, e);
        }
    }

}
