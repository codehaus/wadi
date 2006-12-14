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
import org.codehaus.wadi.location.session.EvacuateIMToPM;
import org.codehaus.wadi.location.session.EvacuatePMToIM;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;


/**
 * 
 * @version $Revision: 1538 $
 */
public class LocalPartitionEvacuateIMToPMActionTest extends AbstractLocalPartitionActionTest {

    public void testEvacueeDoesNotExist() throws Exception {
        recordReply(false);
        
        LocalPartitionEvacuateIMToPMAction action = new LocalPartitionEvacuateIMToPMAction(dispatcher,
                nameToLocation,
                log);
        action.onMessage(envelope, new EvacuateIMToPM("key", peer));
    }

    public void testEvacueeExists() throws Exception {
        Peer peer2 = (Peer) mock(Peer.class);
        
        recordReply(true);
        
        LocalPartitionEvacuateIMToPMAction action = new LocalPartitionEvacuateIMToPMAction(dispatcher,
                nameToLocation,
                log);
        String key = "key";
        nameToLocation.put(key, new Location(key, peer));
        action.onMessage(envelope, new EvacuateIMToPM(key, peer2));
        
        assertTrue(nameToLocation.containsKey(key));
        Location location = (Location) nameToLocation.get(key);
        assertSame(peer2, location.getSMPeer());
    }

    public void testEvacueeExistsYetAlreadyAtRightLocation() throws Exception {
        recordReply(false);
        
        LocalPartitionEvacuateIMToPMAction action = new LocalPartitionEvacuateIMToPMAction(dispatcher,
                nameToLocation,
                log);
        String key = "key";
        nameToLocation.put(key, new Location(key, peer));
        action.onMessage(envelope, new EvacuateIMToPM(key, peer));
    }

    private void recordReply(final boolean success) throws MessageExchangeException {
        dispatcher.reply(envelope, null);
        modify().args(is.AS_RECORDED, new AbstractExpression() {

            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                EvacuatePMToIM result = (EvacuatePMToIM) arg0;
                if (success) {
                    return result.isSuccess();
                } else {
                    return !result.isSuccess();
                }
            }
            
        });
        startVerification();
    }

}
