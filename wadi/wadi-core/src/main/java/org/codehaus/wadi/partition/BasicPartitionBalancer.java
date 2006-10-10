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
package org.codehaus.wadi.partition;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.Quipu;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicPartitionBalancer implements PartitionBalancer {
    private final Dispatcher dispatcher;
    private final int nbPartitions;
    private final ServiceEndpointBuilder endpointBuilder;
    
    public BasicPartitionBalancer(Dispatcher dispatcher, int nbPartitions) {
        if (null == dispatcher) {
            throw new IllegalArgumentException("dispatcher is required");
        } else if (1 > nbPartitions) {
            throw new IllegalArgumentException("nbPartitions must be greater than 0");
        }
        this.dispatcher = dispatcher;
        this.nbPartitions = nbPartitions;

        endpointBuilder = new ServiceEndpointBuilder();
    }

    public void start() throws Exception {
        endpointBuilder.addCallback(dispatcher, PartitionBalancingInfoState.class);
    }

    public void stop() throws Exception {
        endpointBuilder.dispose(10, 500);
    }
    
    public void balancePartitions() throws MessageExchangeException {
        Cluster cluster = dispatcher.getCluster();
        
        Set peers = new HashSet();
        peers.addAll(cluster.getRemotePeers().values());
        peers.add(cluster.getLocalPeer());
        
        Map peerToBalancingState = fetchBalancingInfoState(peers);
        
        PartitionBalancingStrategy balancingStrategy = newBalancingStrategy(nbPartitions, peerToBalancingState);
        PartitionInfoUpdate[] balancingInfoUpdates = balancingStrategy.computePartitionInfoUpdate();
        
        publishBalancingInfoUpdate(peerToBalancingState, balancingInfoUpdates);
    }

    protected Map fetchBalancingInfoState(Set peers) throws MessageExchangeException {
        String correlationId = dispatcher.nextCorrelationId();
        Quipu peerResponseWaitable = dispatcher.setRendezVous(correlationId, peers.size());
        
        for (Iterator iter = peers.iterator(); iter.hasNext();) {
            Peer peer = (Peer) iter.next();
            dispatcher.send(peer.getAddress(), correlationId, new RetrieveBalancingInfoEvent());
        }

        Collection results = dispatcher.attemptMultiRendezVous(correlationId, peerResponseWaitable, 5000);
        Map peerToBalancingInfoState = new HashMap();
        for (Iterator iter = results.iterator(); iter.hasNext();) {
            Envelope replyMsg = (Envelope) iter.next();
            PartitionBalancingInfoState balancingInfoState = (PartitionBalancingInfoState) replyMsg.getPayload();
            peerToBalancingInfoState.put(balancingInfoState.getDefiningPeer(), balancingInfoState);
        }
        
        return peerToBalancingInfoState;
    }

    protected void publishBalancingInfoUpdate(Map peerToBalancingState, PartitionInfoUpdate[] partitionInfoUpdates) 
        throws MessageExchangeException {
        boolean isPartitionManagerAlone = peerToBalancingState.size() == 1;
        for (Iterator iter = peerToBalancingState.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Entry) iter.next();
            Peer peer = (Peer) entry.getKey();
            PartitionBalancingInfoState balancingInfoState = (PartitionBalancingInfoState) entry.getValue();
            boolean evacuatingPartitions = balancingInfoState.isEvacuatingPartitions();
            PartitionBalancingInfoUpdate infoUpdate = 
                new PartitionBalancingInfoUpdate(isPartitionManagerAlone, evacuatingPartitions, partitionInfoUpdates);
            dispatcher.send(peer.getAddress(), infoUpdate);
        }
    }

    protected PartitionBalancingStrategy newBalancingStrategy(int nbPartitions, Map peerToBalancingInfoState) {
        return new BasicEvenBalancer(nbPartitions, peerToBalancingInfoState);
    }
}
