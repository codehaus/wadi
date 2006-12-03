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
package org.codehaus.wadi.location.partition;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.Quipu;
import org.codehaus.wadi.location.impl.LocalPartition;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicPartitionRepopulateTask implements PartitionRepopulateTask {
    private final Dispatcher dispatcher;
    private final LocalPeer localPeer;
    private final long waitForRepopulationTime;
    
    public BasicPartitionRepopulateTask(Dispatcher dispatcher, long waitForRepopulationTime) {
        if (null == dispatcher) {
            throw new IllegalArgumentException("dispatcher is required");
        } else if (0 > waitForRepopulationTime) {
            throw new IllegalArgumentException("waitForRepopulationTime must be >= 0");
        }
        this.dispatcher = dispatcher;
        this.waitForRepopulationTime = waitForRepopulationTime;
        
        localPeer = dispatcher.getCluster().getLocalPeer();
    }

    public void repopulate(LocalPartition[] toBePopulated) throws MessageExchangeException, PartitionRepopulationException {
        Quipu rv = sendRepopulateRequest(toBePopulated);

        try {
            rv.waitFor(waitForRepopulationTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PartitionRepopulationException(e);
        }
        
        Collection results = rv.getResults();
        for (Iterator i = results.iterator(); i.hasNext();) {
            Envelope message = (Envelope) i.next();
            PartitionRepopulateResponse response = (PartitionRepopulateResponse) message.getPayload();
            Map keyToSessionNames = response.getKeyToSessionNames();
            Peer peer = response.getPeer();
            repopulate(toBePopulated, peer, keyToSessionNames);
        }
    }
    
    protected Quipu sendRepopulateRequest(LocalPartition[] toBePopulated) throws MessageExchangeException {
        int[] indicesToRepopulate = new int[toBePopulated.length];
        for (int i = 0; i < indicesToRepopulate.length; i++) {
            indicesToRepopulate[i] = toBePopulated[i].getKey();
        }
        
        PartitionRepopulateRequest repopulateRequest = new PartitionRepopulateRequest(indicesToRepopulate);
        Quipu rv = dispatcher.newRendezVous(dispatcher.getCluster().getPeerCount());
        dispatcher.send(localPeer.getAddress(), 
                dispatcher.getCluster().getAddress(), 
                rv.getCorrelationId(),
                repopulateRequest);
        return rv;
    }

    protected void repopulate(LocalPartition[] toBePopulated, Peer peer, Map keyToSessionNames) {
        for (int i = 0; i < toBePopulated.length; i++) {
            LocalPartition partition = toBePopulated[i];
            Collection sessionNames = (Collection) keyToSessionNames.get(new Integer(partition.getKey()));
            partition.put(sessionNames, peer);
        }
    }
    
}
