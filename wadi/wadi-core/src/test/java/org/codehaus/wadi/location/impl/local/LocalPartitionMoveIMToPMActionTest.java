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
package org.codehaus.wadi.location.impl.local;

import java.io.IOException;

import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.location.impl.local.LocalPartition.Location;
import org.codehaus.wadi.location.session.MoveIMToPM;
import org.codehaus.wadi.location.session.MovePMToIM;
import org.codehaus.wadi.location.session.MovePMToSM;
import org.codehaus.wadi.location.session.MoveSMToPM;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.Expression;
import com.agical.rmock.core.match.operator.AbstractExpression;


/**
 * 
 * @version $Revision: 1538 $
 */
public class LocalPartitionMoveIMToPMActionTest extends AbstractLocalPartitionActionTest {

    public void testSuccessfulSessionRelocation() throws Exception {
        Peer peer2 = (Peer) mock(Peer.class);
        String key = "key";
        int timeout = 10;
        String correlationId = "correlationId";
        recordExchangeSendWithSMAndResult(peer2, key, timeout, correlationId, true);
        startVerification();

        LocalPartitionMoveIMToPMAction action = new LocalPartitionMoveIMToPMAction(dispatcher,
                nameToLocation,
                log,
                timeout);
        nameToLocation.put(key, new Location(key, peer));
        action.onMessage(envelope, new MoveIMToPM(peer2, key, true, false));
        
        assertTrue(nameToLocation.containsKey(key));
        Location location = (Location) nameToLocation.get(key);
        assertSame(peer2, location.getSMPeer());
    }

    public void testSessionRelocationFails() throws Exception {
        Peer peer2 = (Peer) mock(Peer.class);
        String key = "key";
        int timeout = 10;
        String correlationId = "correlationId";
        recordExchangeSendWithSMAndResult(peer2, key, timeout, correlationId, false);
        recordReplyWithUnknownLocation();
        startVerification();

        LocalPartitionMoveIMToPMAction action = new LocalPartitionMoveIMToPMAction(dispatcher,
                nameToLocation,
                log,
                timeout);
        nameToLocation.put(key, new Location(key, peer));
        action.onMessage(envelope, new MoveIMToPM(peer2, key, true, false));
        
        assertFalse(nameToLocation.containsKey(key));
    }

    public void testSessionRelocationFailsUponExchange() throws Exception {
        Peer peer2 = (Peer) mock(Peer.class);
        String key = "key";
        int timeout = 10;
        String correlationId = "correlationId";
        recordExchangeSendWithSM(peer2, key, timeout, correlationId);
        modify().throwException(new MessageExchangeException("fail"));
        recordReplyWithUnknownLocation();
        startVerification();
        
        LocalPartitionMoveIMToPMAction action = new LocalPartitionMoveIMToPMAction(dispatcher,
                nameToLocation,
                log,
                timeout);
        nameToLocation.put(key, new Location(key, peer));
        action.onMessage(envelope, new MoveIMToPM(peer2, key, true, false));
        
        assertFalse(nameToLocation.containsKey(key));
    }

    public void testRelocateUndefinedKey() throws Exception {
        recordReplyWithUnknownLocation();
        startVerification();
        
        LocalPartitionMoveIMToPMAction action = new LocalPartitionMoveIMToPMAction(dispatcher, nameToLocation, log, 10);
        action.onMessage(envelope, new MoveIMToPM(peer, "key", true, false));
    }

    private void recordReplyWithUnknownLocation() throws Exception {
        dispatcher.reply(envelope, null);
        modify().args(is.AS_RECORDED, new AbstractExpression() {

            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                return arg0 instanceof MovePMToIM;
            }
            
        });
    }
    
    private void recordExchangeSendWithSMAndResult(final Peer peer2,
            final String key,
            int timeout,
            final String correlationId,
            boolean success) throws MessageExchangeException {
        recordExchangeSendWithSM(peer2, key, timeout, correlationId);
        modify().returnValue(envelope);
        envelope.getPayload();
        modify().returnValue(new MoveSMToPM(success));
    }

    private void recordExchangeSendWithSM(final Peer peer2, final String key, int timeout, final String correlationId)
            throws MessageExchangeException {
        envelope.getSourceCorrelationId();
        modify().returnValue(correlationId);

        dispatcher.exchangeSend(peerAddress, null, timeout);
        modify().args(new Expression[] { is.AS_RECORDED, new AbstractExpression() {

            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                MovePMToSM msg = (MovePMToSM) arg0;
                if (!msg.getKey().equals(key)) {
                    return false;
                } else if (msg.getIMPeer() != peer2) {
                    return false;
                } else if (!msg.getIMCorrelationId().equals(correlationId)) {
                    return false;
                }
                return true;
            }

        }, is.AS_RECORDED });
    }
    
}
