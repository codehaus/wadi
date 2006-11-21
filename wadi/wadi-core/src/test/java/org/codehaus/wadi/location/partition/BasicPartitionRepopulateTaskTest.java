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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.Quipu;
import org.codehaus.wadi.location.impl.LocalPartition;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.Expression;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicPartitionRepopulateTaskTest extends RMockTestCase {

    private Dispatcher dispatcher;
    private Address clusterAddress;
    private Address localPeerAddress;
    private Peer peer2;
    private Peer peer3;

    protected void setUp() throws Exception {
        dispatcher = (Dispatcher) mock(Dispatcher.class);

        Cluster cluster = dispatcher.getCluster();
        modify().multiplicity(expect.atLeast(0));

        clusterAddress = cluster.getAddress();
        modify().multiplicity(expect.atLeast(0));

        cluster.getPeerCount();
        modify().multiplicity(expect.atLeast(0)).returnValue(2);

        LocalPeer localPeer = cluster.getLocalPeer();
        modify().multiplicity(expect.atLeast(0));

        localPeer.getName();
        modify().multiplicity(expect.atLeast(0)).returnValue("name");
        
        localPeerAddress = localPeer.getAddress();
        modify().multiplicity(expect.atLeast(0));
        
        peer2 = (Peer) mock(Peer.class);
        peer3 = (Peer) mock(Peer.class);
    }

    public void testRepopulationOK() throws Exception {
        dispatcher.newRendezVous(2);
        final Quipu quipu = new Quipu(2, "corrId");
        modify().returnValue(quipu);

        final Envelope envelopePeer2 = (Envelope) mock(Envelope.class);
        envelopePeer2.getPayload();
        modify().returnValue(newResponse("peer2", peer2));
        
        final Envelope envelopePeer3 = (Envelope) mock(Envelope.class);
        envelopePeer3.getPayload();
        modify().returnValue(newResponse("peer3", peer3));

        dispatcher.send(localPeerAddress, clusterAddress, quipu.getCorrelationId(), null);
        modify().args(new Expression[] {is.AS_RECORDED,  is.AS_RECORDED, is.AS_RECORDED, new AbstractExpression() {

            public void describeWith(ExpressionDescriber expressionDescriber) throws IOException {
            }

            public boolean passes(Object object) {
                PartitionRepopulateRequest request = (PartitionRepopulateRequest) object;
                int[] keys = request.getKeys();
                assertEquals(2, keys.length);
                assertEquals(1, keys[0]);
                assertEquals(3, keys[1]);
                quipu.putResult(envelopePeer2);
                quipu.putResult(envelopePeer3);
                return true;
            }


        }});

        startVerification();

        BasicPartitionRepopulateTask repopulateTask = new BasicPartitionRepopulateTask(dispatcher, 200);

        LocalPartition partition1 = new LocalPartition(dispatcher, 1, 100);
        LocalPartition partition3 = new LocalPartition(dispatcher, 3, 100);
        LocalPartition[] localPartitions = new LocalPartition[] {partition1, partition3};
        repopulateTask.repopulate(localPartitions);
        
        Map nameToLocation = partition1.getNameToLocation();
        assertEquals(4, nameToLocation.size());

        nameToLocation = partition3.getNameToLocation();
        assertEquals(2, nameToLocation.size());
    }

    public void testRepopulationWithoutResponse() throws Exception {
        dispatcher.newRendezVous(2);
        final Quipu quipu = new Quipu(2, "corrId");
        modify().returnValue(quipu);

        dispatcher.send(localPeerAddress, clusterAddress, quipu.getCorrelationId(), null);
        modify().args(new Expression[] {is.AS_RECORDED,  is.AS_RECORDED, is.AS_RECORDED, is.ANYTHING});

        startVerification();

        BasicPartitionRepopulateTask repopulateTask = new BasicPartitionRepopulateTask(dispatcher, 100);

        LocalPartition partition1 = new LocalPartition(dispatcher, 1, 100);
        LocalPartition partition3 = new LocalPartition(dispatcher, 3, 100);
        LocalPartition[] localPartitions = new LocalPartition[] {partition1, partition3};
        repopulateTask.repopulate(localPartitions);
        
        Map nameToLocation = partition1.getNameToLocation();
        assertEquals(0, nameToLocation.size());

        nameToLocation = partition3.getNameToLocation();
        assertEquals(0, nameToLocation.size());
    }

    private PartitionRepopulateResponse newResponse(String prefix, Peer peer) {
        Map keyToSessionNames = new HashMap();
        Collection sessionNames = new ArrayList();
        sessionNames.add(prefix + "Key1name1");
        sessionNames.add(prefix + "Key1name2");
        keyToSessionNames.put(new Integer(1), sessionNames);
        sessionNames = new ArrayList();
        sessionNames.add(prefix + "Key2name1");
        keyToSessionNames.put(new Integer(3), sessionNames);
        PartitionRepopulateResponse response = new PartitionRepopulateResponse(peer, keyToSessionNames);
        return response;
    }

}
