/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
package org.codehaus.wadi.location.impl;

import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.location.newmessages.DeleteIMToPM;
import org.codehaus.wadi.location.newmessages.EvacuateIMToPM;
import org.codehaus.wadi.location.newmessages.InsertIMToPM;
import org.codehaus.wadi.location.newmessages.MoveIMToPM;
import org.codehaus.wadi.location.newmessages.MovePMToSM;
import org.codehaus.wadi.location.newmessages.PutSMToIM;
import org.codehaus.wadi.location.newmessages.ReleaseEntryRequest;

/**
 * 
 * @version $Revision: 1603 $
 */
public interface StateManagerMessageListener {
    void onDIndexInsertionRequest(Message om, InsertIMToPM request);

    void onDIndexDeletionRequest(Message om, DeleteIMToPM request);

    void onDIndexRelocationRequest(Message om, EvacuateIMToPM request);

    void onMessage(Message message, MoveIMToPM request);

    // called on State Master...
    void onMessage(Message message1, MovePMToSM request);

    void onEmigrationRequest(Message message, ReleaseEntryRequest request);

    void onPutSMToIM(Message message, PutSMToIM request);
}