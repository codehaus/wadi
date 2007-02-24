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
import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.contextualiser.InvocationException;
import org.codehaus.wadi.core.motable.AbstractMotable;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.motable.SimpleMotable;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.location.session.MoveIMToSM;
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
    private final long inactiveTime;
    private final ServiceEndpointBuilder endpointBuilder;

    public MovePMToSMEndPoint(ServiceSpace serviceSpace,
            Contextualiser contextualiser,
            long inactiveTime) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == contextualiser) {
            throw new IllegalArgumentException("contextualiser is required");
        }
        this.contextualiser = contextualiser;
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
            RelocationImmoter promoter = new RelocationImmoter(message, request);
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

    /**
     * We receive a RelocationRequest and pass a RelocationImmoter down the
     * Contextualiser stack. The Session is passed to us through the Immoter and
     * we pass it back to the Request-ing node...
     * 
     * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
     * @version $Revision:1815 $
     */
    class RelocationImmoter implements Immoter {
        protected final Envelope message;
        protected final MovePMToSM request;
        protected boolean found = false;

        public RelocationImmoter(Envelope message, MovePMToSM request) {
            this.message = message;
            this.request = request;
        }

        public Motable newMotable() {
            return new PMToIMEmotable(message, request);
        }

        public boolean immote(Motable emotable, Motable immotable) {
            found = true;
            return true;
        }
        
        public boolean contextualise(Invocation invocation, String id, Motable immotable)
                throws InvocationException {
            return false;
        }

        public boolean isFound() {
            return found;
        }

    }

    class PMToIMEmotable extends AbstractMotable {
        private final Envelope message;
        private final MovePMToSM get;

        public PMToIMEmotable(Envelope message, MovePMToSM get) {
            this.message = message;
            this.get = get;
        }

        public byte[] getBodyAsByteArray() throws Exception {
            throw new UnsupportedOperationException();
        }

        public void setBodyAsByteArray(byte[] bytes) throws Exception {
            Motable immotable = new SimpleMotable();
            immotable.init(creationTime, lastAccessedTime, maxInactiveInterval, name);
            immotable.setBodyAsByteArray(bytes);

            LocalPeer smPeer = dispatcher.getCluster().getLocalPeer();
            Peer imPeer = get.getIMPeer();
            MoveSMToIM request = new MoveSMToIM(immotable);
            // send on state from StateMaster to InvocationMaster...
            if (log.isTraceEnabled()) {
                log.trace("exchanging MoveSMToIM between [" + smPeer + "]->[" + imPeer + "]");
            }
            Envelope message2 = dispatcher.exchangeSend(imPeer.getAddress(), request, inactiveTime, get
                    .getIMCorrelationId());
            // should receive response from IM confirming safe receipt...
            if (message2 == null) {
                // TODO throw exception
                log.error("NO REPLY RECEIVED FOR MESSAGE IN TIMEFRAME - PANIC!");
            } else {
                MoveIMToSM response = (MoveIMToSM) message2.getPayload();
                assert (response != null && response.getSuccess()); 
                dispatcher.reply(message, new MoveSMToPM(true));
            }
        }

    }

}
