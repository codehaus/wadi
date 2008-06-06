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
package org.codehaus.wadi.location.partitionmanager.local;

import java.io.IOException;

import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
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

    private Peer peer2;
    private String key;
    private int timeout;
    private String correlationId;
    private LocalPartitionMoveIMToPMAction action;

    protected void setUp() throws Exception {
        super.setUp();
        
        peer2 = (Peer) mock(Peer.class);
        key = "key";
        timeout = 10;
        correlationId = "correlationId";
        
        action = new LocalPartitionMoveIMToPMAction(dispatcher, nameToLocation, log);
        nameToLocation.put(key, new Location(key, peer));
    }
    
    public void testSuccessfulSessionRelocation() throws Exception {
        recordExchangeSendWithSMAndResult(true, false);
        startVerification();

        action.onMessage(envelope, new MoveIMToPM(peer2, key, true, false, timeout));
        
        assertTrue(nameToLocation.containsKey(key));
        Location location = (Location) nameToLocation.get(key);
        assertSame(peer2, location.getSMPeer());
    }

    public void testSessionRelocationFails() throws Exception {
        recordExchangeSendWithSMAndResult(false, false);
        recordReplyWithUnknownLocation();
        startVerification();

        action.onMessage(envelope, new MoveIMToPM(peer2, key, true, false, timeout));
        
        assertFalse(nameToLocation.containsKey(key));
    }

    public void testSessionRelocationFailsWithMotableBusyDoesNotUpdateLocation() throws Exception {
        recordExchangeSendWithSMAndResult(false, true);
        startVerification();

        action.onMessage(envelope, new MoveIMToPM(peer2, key, true, false, timeout));
        
        assertTrue(nameToLocation.containsKey(key));
    }

    public void testSessionRelocationFailsUponExchange() throws Exception {
        recordExchangeSendWithSM();
        modify().throwException(new MessageExchangeException("fail"));
        recordReplyWithUnknownLocation();
        startVerification();
        
        action.onMessage(envelope, new MoveIMToPM(peer2, key, true, false, timeout));
        
        assertFalse(nameToLocation.containsKey(key));
    }

    public void testRelocateUndefinedKey() throws Exception {
        recordReplyWithUnknownLocation();
        startVerification();
        
        action.onMessage(envelope, new MoveIMToPM(peer, "unknownKey", true, false, timeout));
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
    
    private void recordExchangeSendWithSMAndResult(boolean success, boolean sessionBuzy) throws MessageExchangeException {
        recordExchangeSendWithSM();
        modify().returnValue(envelope);
        envelope.getPayload();
        modify().returnValue(new MoveSMToPM(success, sessionBuzy));
    }

    private void recordExchangeSendWithSM() throws MessageExchangeException {
        envelope.getSourceCorrelationId();
        modify().returnValue(correlationId);

        dispatcher.exchangeSend(peerAddress, null, timeout + 5000);
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
