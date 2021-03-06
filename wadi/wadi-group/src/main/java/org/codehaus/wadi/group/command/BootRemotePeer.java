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
package org.codehaus.wadi.group.command;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.group.impl.EnvelopeHelper;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;


/**
 * 
 * @version $Revision: 1538 $
 */
public class BootRemotePeer implements ClusterCommand {
    private static final Log log = LogFactory.getLog(BootRemotePeer.class);
    
    private transient final Peer targetPeer;
    private transient final Dispatcher dispatcher;

    public BootRemotePeer(Cluster cluster, Peer targetPeer) {
        if (null == cluster) {
            throw new IllegalArgumentException("cluster is required");
        } else if (null == targetPeer) {
            throw new IllegalArgumentException("targetPeer is required");
        }
        this.targetPeer = targetPeer;
        
        dispatcher = cluster.getDispatcher();
        addCallback();
    }

    public void execute(Envelope envelope, Cluster cluster) {
        LocalPeer localPeer = cluster.getLocalPeer();
        try {
            cluster.getDispatcher().reply(envelope, new BootPeerResponse(localPeer));
        } catch (MessageExchangeException e) {
            log.error(e);
        }
    }

    public Peer getSerializedPeer() {
        ServiceEndpointBuilder endpointBuilder = new ServiceEndpointBuilder();
        Envelope message = null;
        try {
            message = dispatcher.exchangeSend(targetPeer.getAddress(), this, 5000);
        } catch (MessageExchangeException e) {
            log.error("Cannot send command to joining peer [" + targetPeer + "]", e);
            return null;
        } finally {
            endpointBuilder.dispose(10, 500);
        }
        if (null == message) {
            log.error("No command response from peer [" + targetPeer + "]");
            return null;
        }
        BootPeerResponse peerResponse = (BootPeerResponse) message.getPayload();
        return peerResponse.getPeer();
    }
    
    protected void addCallback() {
        dispatcher.register(new ServiceEndpoint() {

            public void dispatch(Envelope envelope) throws Exception {
                dispatcher.addRendezVousEnvelope(envelope);
            }

            public void dispose(int nbAttemp, long delayMillis) {
            }

            public boolean testDispatchEnvelope(Envelope envelope) {
                boolean reply = EnvelopeHelper.isReply(envelope);
                if (!reply) {
                    return false;
                }
                Serializable payload = envelope.getPayload();
                return payload instanceof BootPeerResponse;
            }
            
        });
    }

}