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
import org.codehaus.wadi.location.partitionmanager.local.LocalPartitionInsertIMToPMAction;
import org.codehaus.wadi.location.partitionmanager.local.LocalPartition.Location;
import org.codehaus.wadi.location.session.InsertIMToPM;
import org.codehaus.wadi.location.session.InsertPMToIM;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;

/**
 * 
 * @version $Revision: 1538 $
 */
public class LocalPartitionInsertIMToPMActionTest extends AbstractLocalPartitionActionTest {

    public void testKeyIsNotInUse() throws Exception {
        recordReply(true);
        
        LocalPartitionInsertIMToPMAction action = new LocalPartitionInsertIMToPMAction(dispatcher, nameToLocation, log);
        String key = "key";
        action.onMessage(envelope, new InsertIMToPM(key, peer));
        
        assertTrue(nameToLocation.containsKey(key));
        Location location = (Location) nameToLocation.get(key);
        assertSame(peer, location.getSMPeer());
    }

    public void testKeyIsAlreadyInUse() throws Exception {
        Peer peer2 = (Peer) mock(Peer.class);
        
        recordReply(false);
        
        LocalPartitionInsertIMToPMAction action = new LocalPartitionInsertIMToPMAction(dispatcher, nameToLocation, log);
        String key = "key";
        nameToLocation.put(key, new Location(key, peer));
        action.onMessage(envelope, new InsertIMToPM(key, peer2));
        
        assertTrue(nameToLocation.containsKey(key));
        Location location = (Location) nameToLocation.get(key);
        assertSame(peer, location.getSMPeer());
    }

    private void recordReply(final boolean success) throws MessageExchangeException {
        dispatcher.reply(envelope, null);
        modify().args(is.AS_RECORDED, new AbstractExpression() {

            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                InsertPMToIM result = (InsertPMToIM) arg0;
                if (success) {
                    return result.getSuccess();
                } else {
                    return !result.getSuccess();
                }
            }
            
        });
        startVerification();
    }

}
