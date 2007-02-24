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
package org.codehaus.wadi.location.impl;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.PartitionMapper;
import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.location.partition.PartitionRepopulateRequest;
import org.codehaus.wadi.location.partition.PartitionRepopulateResponse;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpace;

/**
 * 
 * @version $Revision: 1538 $
 */
public class PartitionRepopulationEndPoint implements Lifecycle, PartitionRepopulationEndPointMessageListener {
    public static final ServiceName NAME = new ServiceName("PartitionRepopulationEndPoint");
    
    private static final Log log = LogFactory.getLog(SimpleStateManager.class);
    
    private final Contextualiser contextualiser;
    private final Dispatcher dispatcher;
    private final LocalPeer localPeer;
    private final PartitionMapper mapper;
    private final ServiceEndpointBuilder endpointBuilder;
    
    public PartitionRepopulationEndPoint(ServiceSpace serviceSpace, PartitionMapper mapper, Contextualiser contextualiser) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == mapper) {
            throw new IllegalArgumentException("mapper is required");
        } else if (null == contextualiser) {
            throw new IllegalArgumentException("contextualiser is required");
        }
        this.mapper = mapper;
        this.contextualiser = contextualiser;
        
        dispatcher = serviceSpace.getDispatcher();
        localPeer = dispatcher.getCluster().getLocalPeer();
        endpointBuilder = new ServiceEndpointBuilder();
    }
    
    public void start() throws Exception {
        endpointBuilder.addSEI(dispatcher, PartitionRepopulationEndPointMessageListener.class, this);
    }

    public void stop() throws Exception {
        endpointBuilder.dispose(10, 500);
    }
    
    public void onPartitionRepopulateRequest(Envelope om, PartitionRepopulateRequest request) {
        Map keyToSessionNames = request.createKeyToSessionNames();
        contextualiser.findRelevantSessionNames(mapper, keyToSessionNames);
        try {
            dispatcher.reply(om, new PartitionRepopulateResponse(localPeer, keyToSessionNames));
        } catch (MessageExchangeException e) {
            log.warn("unexpected problem responding to partition repopulation request", e);
        }
    }

}
