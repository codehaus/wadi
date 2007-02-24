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

import java.io.IOException;

import org.codehaus.wadi.core.contextualiser.Invocation;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.location.Partition;
import org.codehaus.wadi.location.PartitionManager;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.MoveIMToSM;
import org.codehaus.wadi.location.session.MoveSMToIM;
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
    private PartitionManager manager;
    private Invocation invocation;
    private Immoter immoter;
    private Dispatcher dispatcher;
    private Cluster cluster;
    private LocalPeer localPeer;

    protected void setUp() throws Exception {
        serviceSpace = (ServiceSpace) mock(ServiceSpace.class);
        dispatcher = serviceSpace.getDispatcher();
        modify().multiplicity(expect.atLeast(0));
        cluster = dispatcher.getCluster();
        modify().multiplicity(expect.atLeast(0));
        localPeer = cluster.getLocalPeer();
        modify().multiplicity(expect.atLeast(0));
        
        manager = (PartitionManager) mock(PartitionManager.class);
        invocation = (Invocation) mock(Invocation.class);
        immoter = (Immoter) mock(Immoter.class);
    }
    
    public void testRelocatedSessionIsNull() throws Exception {
        final String key = "key";
        int timeout = 100;
        boolean shuttingDown = false;
        boolean relocatable = true;
        Partition partition = manager.getPartition(key);
        recordPartitionExchange(key, timeout, shuttingDown, relocatable, partition);
        Envelope envelope = (Envelope) mock(Envelope.class);
        modify().returnValue(envelope);
        envelope.getPayload();
        modify().returnValue(new MoveSMToIM(null));

        dispatcher.reply(envelope, null);
        modify().args(is.AS_RECORDED, new AbstractExpression() {

            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                MoveIMToSM arg = (MoveIMToSM) arg0;
                return !arg.getSuccess();
            }
            
        });
        startVerification();
        
        HybridRelocater relocater = new HybridRelocater(serviceSpace, manager, timeout);
        boolean relocated = relocater.relocate(invocation, key, immoter, shuttingDown);
        assertFalse(relocated);
    }

    private void recordPartitionExchange(final String key,
            int timeout,
            boolean shuttingDown,
            boolean relocatable,
            Partition partition) throws MessageExchangeException {
        invocation.isRelocatable();
        modify().returnValue(relocatable);
        
        new MoveIMToPM(localPeer, key, !shuttingDown, relocatable);
        partition.exchange(null, timeout);
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
