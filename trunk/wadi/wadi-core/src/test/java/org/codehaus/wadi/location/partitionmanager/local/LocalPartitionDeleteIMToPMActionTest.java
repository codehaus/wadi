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
import org.codehaus.wadi.location.partitionmanager.local.LocalPartitionDeleteIMToPMAction;
import org.codehaus.wadi.location.partitionmanager.local.LocalPartition.Location;
import org.codehaus.wadi.location.session.DeleteIMToPM;
import org.codehaus.wadi.location.session.DeletePMToIM;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;


/**
 * 
 * @version $Revision: 1538 $
 */
public class LocalPartitionDeleteIMToPMActionTest extends AbstractLocalPartitionActionTest {

    public void testDeleteDefinedKey() throws Exception {
        recordReply(true);
        
        LocalPartitionDeleteIMToPMAction action = new LocalPartitionDeleteIMToPMAction(dispatcher, nameToLocation, log);
        String key = "key";
        nameToLocation.put(key, new Location(key, peer));
        action.onMessage(envelope, new DeleteIMToPM(key));
        
        assertFalse(nameToLocation.containsKey(key));
    }

    public void testDeleteUndefinedKey() throws Exception {
        recordReply(false);
        
        LocalPartitionDeleteIMToPMAction action = new LocalPartitionDeleteIMToPMAction(dispatcher, nameToLocation, log);
        action.onMessage(envelope, new DeleteIMToPM("key"));
    }

    private void recordReply(final boolean success) throws MessageExchangeException {
        dispatcher.reply(envelope, null);
        modify().args(is.AS_RECORDED, new AbstractExpression() {

            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                DeletePMToIM result = (DeletePMToIM) arg0;
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
