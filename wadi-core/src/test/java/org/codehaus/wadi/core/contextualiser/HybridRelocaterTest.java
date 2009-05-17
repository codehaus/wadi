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
package org.codehaus.wadi.core.contextualiser;

import java.io.IOException;

import org.codehaus.wadi.core.MotableBusyException;
import org.codehaus.wadi.core.motable.Emoter;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.location.partitionmanager.Partition;
import org.codehaus.wadi.location.partitionmanager.PartitionManager;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.MoveIMToSM;
import org.codehaus.wadi.location.session.MoveSMToIM;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.servicespace.ServiceSpace;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class HybridRelocaterTest extends RMockTestCase {

    private ServiceSpace serviceSpace;
    private PartitionManager partitionManager;
    private Invocation invocation;
    private long exclusiveSessionLockWaitTime;
    private Immoter immoter;
    private Dispatcher dispatcher;
    private Cluster cluster;
    private LocalPeer localPeer;
    private String key;
    private ReplicationManager replicationManager;
    private Immoter sessionRelocationImmoter;

    protected void setUp() throws Exception {
        serviceSpace = (ServiceSpace) mock(ServiceSpace.class);
        dispatcher = serviceSpace.getDispatcher();
        modify().multiplicity(expect.atLeast(0));
        cluster = dispatcher.getCluster();
        modify().multiplicity(expect.atLeast(0));
        localPeer = cluster.getLocalPeer();
        modify().multiplicity(expect.atLeast(0));
        
        partitionManager = (PartitionManager) mock(PartitionManager.class);
        replicationManager = (ReplicationManager) mock(ReplicationManager.class);
        
        invocation = (Invocation) mock(Invocation.class);
        invocation.getExclusiveSessionLockWaitTime();
        exclusiveSessionLockWaitTime = 1000;
        modify().returnValue(exclusiveSessionLockWaitTime);
        
        immoter = (Immoter) mock(Immoter.class);
        sessionRelocationImmoter = (Immoter) mock(Immoter.class);
        
        key = "key";
    }
    
    public void testSuccessfulRelocationWithReplicaInfo() throws Exception {
        boolean shuttingDown = false;
        boolean relocatable = true;
        
        Motable relocatedMotable = (Motable) mock(Motable.class);
        
        Partition partition = partitionManager.getPartition(key);
        recordPartitionExchange(shuttingDown, relocatable, partition);
        Envelope envelope = (Envelope) mock(Envelope.class);
        modify().returnValue(envelope);
        envelope.getPayload();
        
        ReplicaInfo replicaInfo = (ReplicaInfo) intercept(ReplicaInfo.class, new Object[] {localPeer, new Peer[0], relocatedMotable}, "ReplicaInfo");
        modify().returnValue(new MoveSMToIM(relocatedMotable, replicaInfo));

        Motable motedNewMotable = sessionRelocationImmoter.newMotable(relocatedMotable);
        
        recordReplyToSM(envelope, true);
        
        replicationManager.insertReplicaInfo(key, replicaInfo);
        
        sessionRelocationImmoter.contextualise(invocation, key, motedNewMotable);
        modify().returnValue(true);
        
        startVerification();
        
        HybridRelocater relocater = newMockedHybridRelocater();
        boolean relocated = relocater.relocate(invocation, key, immoter, shuttingDown);

        assertTrue(relocated);
    }

    public void testSuccessfulRelocationWithoutReplicaInfo() throws Exception {
        boolean shuttingDown = false;
        boolean relocatable = true;
        
        Motable relocatedMotable = (Motable) mock(Motable.class);
        
        Partition partition = partitionManager.getPartition(key);
        recordPartitionExchange(shuttingDown, relocatable, partition);
        Envelope envelope = (Envelope) mock(Envelope.class);
        modify().returnValue(envelope);
        envelope.getPayload();
        
        modify().returnValue(new MoveSMToIM(relocatedMotable, null));
        
        Motable motedNewMotable = sessionRelocationImmoter.newMotable(relocatedMotable);
        
        recordReplyToSM(envelope, true);
        
        sessionRelocationImmoter.contextualise(invocation, key, motedNewMotable);
        modify().returnValue(true);

        startVerification();
        
        HybridRelocater relocater = newMockedHybridRelocater();
        boolean relocated = relocater.relocate(invocation, key, immoter, shuttingDown);

        assertTrue(relocated);
    }
    
    public void testRelocatedSessionIsNull() throws Exception {
        boolean shuttingDown = false;
        boolean relocatable = true;
        
        Partition partition = partitionManager.getPartition(key);
        recordPartitionExchange(shuttingDown, relocatable, partition);
        Envelope envelope = (Envelope) mock(Envelope.class);
        modify().returnValue(envelope);
        envelope.getPayload();
        modify().returnValue(new MoveSMToIM());
        startVerification();
        
        HybridRelocater relocater = new HybridRelocater(serviceSpace, partitionManager, replicationManager);
        boolean relocated = relocater.relocate(invocation, key, immoter, shuttingDown);
        assertFalse(relocated);
    }
    
    public void testThrowExceptionIsMotableToBeRelocatedIsBusy() throws Exception {
        boolean shuttingDown = false;
        boolean relocatable = true;
        
        Partition partition = partitionManager.getPartition(key);
        recordPartitionExchange(shuttingDown, relocatable, partition);
        Envelope envelope = (Envelope) mock(Envelope.class);
        modify().returnValue(envelope);
        envelope.getPayload();
        modify().returnValue(new MoveSMToIM(true));

        startVerification();
        
        HybridRelocater relocater = new HybridRelocater(serviceSpace, partitionManager, replicationManager);
        try {
            relocater.relocate(invocation, key, immoter, shuttingDown);
            fail();
        } catch (MotableBusyException e) {
        }
    }

    private HybridRelocater newMockedHybridRelocater() {
        return new HybridRelocater(serviceSpace, partitionManager, replicationManager) {
            @Override
            protected Immoter newSessionRelocationImmoter(Invocation invocation, Immoter immoter) {
                return sessionRelocationImmoter;
            }
            @Override
            protected Motable mote(String name, Immoter immoter, Motable emotable, Emoter emoter) {
                return immoter.newMotable(emotable);
            }
        };
    }

    private void recordReplyToSM(Envelope envelope, final boolean success) throws MessageExchangeException {
        dispatcher.reply(envelope, null);
        modify().args(is.AS_RECORDED, new AbstractExpression() {

            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                MoveIMToSM arg = (MoveIMToSM) arg0;
                return success == arg.getSuccess();
            }
            
        });
    }

    private void recordPartitionExchange(boolean shuttingDown,
            boolean relocatable,
            Partition partition) throws MessageExchangeException {
        invocation.isRelocatable();
        modify().returnValue(relocatable);
        
        partition.exchange(null, exclusiveSessionLockWaitTime + 10000);
        modify().args(new AbstractExpression() {

            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                MoveIMToPM arg= (MoveIMToPM) arg0;
                if (arg.getIMPeer() != localPeer) {
                    return false;
                } else if (!arg.getKey().equals(key)) {
                    return false;
                }
                return true;
            }
            
        }, is.AS_RECORDED);
    }

}
