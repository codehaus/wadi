/**
 *
 * Copyright ...
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
package org.codehaus.wadi.jgroups;

import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.jgroups.messages.StateRequest;
import org.codehaus.wadi.jgroups.messages.StateUpdate;

/**
 * When a Peer becomes aware of the existance of another Peer, this protocol
 * is used to exchange data between them, so that each is informed of the other's
 * public state.
 *
 * @version $Revision: 1757 $
 */
public interface JGroupsClusterMessageListener {

    void onMessage(Message message, StateUpdate update) throws Exception;
    void onMessage(Message message, StateRequest request) throws Exception;

}