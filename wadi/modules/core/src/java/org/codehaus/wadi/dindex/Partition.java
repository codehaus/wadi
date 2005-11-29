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
package org.codehaus.wadi.dindex;

import javax.jms.ObjectMessage;

import org.codehaus.wadi.PMPartition;
import org.codehaus.wadi.dindex.messages.DIndexDeletionRequest;
import org.codehaus.wadi.dindex.messages.DIndexForwardRequest;
import org.codehaus.wadi.dindex.messages.DIndexRelocationRequest;
import org.codehaus.wadi.dindex.newmessages.InsertIMToPM;
import org.codehaus.wadi.dindex.newmessages.MoveIMToPM;

public interface Partition extends PMPartition, SMPartition {

	void onMessage(ObjectMessage message, InsertIMToPM request);
	void onMessage(ObjectMessage message, DIndexDeletionRequest request);
	void onMessage(ObjectMessage message, DIndexRelocationRequest request);
	void onMessage(ObjectMessage message, DIndexForwardRequest request);
	void onMessage(ObjectMessage message, MoveIMToPM request);

	ObjectMessage exchange(DIndexRequest request, long timeout) throws Exception;

}