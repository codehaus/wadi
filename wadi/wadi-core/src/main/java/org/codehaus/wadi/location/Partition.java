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
package org.codehaus.wadi.location;

import org.codehaus.wadi.PMPartition;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.location.newmessages.DeleteIMToPM;
import org.codehaus.wadi.location.newmessages.EvacuateIMToPM;
import org.codehaus.wadi.location.newmessages.InsertIMToPM;
import org.codehaus.wadi.location.newmessages.MoveIMToPM;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public interface Partition extends PMPartition, SMPartition {
	void onMessage(Message message, InsertIMToPM request);

    void onMessage(Message message, DeleteIMToPM request);
	
    void onMessage(Message message, EvacuateIMToPM request);
	
    void onMessage(Message message, MoveIMToPM request);

	Message exchange(DIndexRequest request, long timeout) throws Exception;
}
