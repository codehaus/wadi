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
package org.codehaus.wadi.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Lifecycle;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.location.StateManager;
import org.codehaus.wadi.location.session.ReleaseEntryRequest;
import org.codehaus.wadi.location.session.ReleaseEntryResponse;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpace;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ReleaseEntryRequestEndPoint implements Lifecycle, ReleaseEntryRequestEndPointMessageListener {
    public static final ServiceName NAME = new ServiceName("ReleaseEntryRequestEndPoint");

    private final static Log log = LogFactory.getLog(ReleaseEntryRequestEndPoint.class);
    
    private final ServiceSpace serviceSpace; 
    private final Contextualiser contextualiser;
    private final StateManager stateManager;
    private final ServiceEndpointBuilder endpointBuilder;
    
    public ReleaseEntryRequestEndPoint(ServiceSpace serviceSpace, Contextualiser contextualiser, StateManager stateManager) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == contextualiser) {
            throw new IllegalArgumentException("contextualiser is required");
        } else if (null == stateManager) {
            throw new IllegalArgumentException("stateManager is required");
        }
        this.serviceSpace = serviceSpace;
        this.contextualiser = contextualiser;
        this.stateManager = stateManager;

        endpointBuilder = new ServiceEndpointBuilder();
    }
    
    public void start() throws Exception {
        endpointBuilder.addSEI(serviceSpace.getDispatcher(), ReleaseEntryRequestEndPointMessageListener.class, this);
    }

    public void stop() throws Exception {
        endpointBuilder.dispose(10, 500);
    }
    
    public void onReleaseEntryRequest(Envelope message, ReleaseEntryRequest request) {
        Motable emotable = request.getMotable();
        String name = emotable.getName();
        Emoter emoter = new ReleaseEntryRequestEmoter(message);
        Immoter immoter = contextualiser.getDemoter(name, emotable);
        immoter = new RehydrationImmoter(immoter, emotable);
        Utils.mote(emoter, immoter, emotable, name);
        stateManager.relocate(name);
    }
    
    /**
     * Manage the immigration of a session from another node and and thence its
     * emotion from the cluster layer into another.
     * 
     * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
     * @version $Revision: 2175 $
     */
    class ReleaseEntryRequestEmoter extends AbstractChainedEmoter {
        protected final Envelope _message;

        public ReleaseEntryRequestEmoter(Envelope message) {
            _message = message;
        }

        public boolean emote(Motable emotable, Motable immotable) {
            if (super.emote(emotable, immotable)) {
                try {
                    serviceSpace.getDispatcher().reply(_message, new ReleaseEntryResponse(true));
                } catch (MessageExchangeException e) {
                    log.warn("Cannot acknowledge immigration request", e);
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }
    }

}
