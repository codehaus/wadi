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
import org.codehaus.wadi.location.SessionRequestMessage;
import org.codehaus.wadi.location.session.DeleteIMToPM;
import org.codehaus.wadi.location.session.EvacuateIMToPM;
import org.codehaus.wadi.location.session.InsertIMToPM;
import org.codehaus.wadi.location.session.MoveIMToPM;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision:1815 $
 */
public class UnknownPartition extends AbstractPartition {

    public UnknownPartition(int key) {
        super(key);
    }

    // 'java.lang.Object' API

    public String toString() {
        return "<unknown>";
    }

    // 'Partition' API

    public boolean isLocal() {
        return false;
    }

    // incoming ...

    public void onMessage(Message message, InsertIMToPM request) {
        throw new UnsupportedOperationException();
    }

    public void onMessage(Message message, DeleteIMToPM request) {
        throw new UnsupportedOperationException();
    }

    public void onMessage(Message message, EvacuateIMToPM request) {
        throw new UnsupportedOperationException();
    }

    public void onMessage(Message message, MoveIMToPM request) {
        throw new UnsupportedOperationException();
    }

    // outgoing...

    public Message exchange(SessionRequestMessage request, long timeout) throws Exception {
        throw new UnsupportedOperationException();
    }

}
