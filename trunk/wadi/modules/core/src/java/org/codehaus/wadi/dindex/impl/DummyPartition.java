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
package org.codehaus.wadi.dindex.impl;

import javax.jms.ObjectMessage;

import org.codehaus.wadi.dindex.DIndexRequest;
import org.codehaus.wadi.dindex.messages.DIndexForwardRequest;
import org.codehaus.wadi.dindex.messages.DIndexRelocationRequest;
import org.codehaus.wadi.dindex.newmessages.DeleteIMToPM;
import org.codehaus.wadi.dindex.newmessages.InsertIMToPM;
import org.codehaus.wadi.dindex.newmessages.MoveIMToPM;
import org.codehaus.wadi.dindex.newmessages.MovePMToSM;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DummyPartition extends AbstractPartition {

    public DummyPartition(int key) {
        super(key);
    }

    protected DummyPartition() {
        super();
        // for deserialisation...
        throw new UnsupportedOperationException();
    }

    public boolean isLocal() {
        return false;
    }

	public String toString() {
        return "<unknown>";
    }

    public void dispatch(ObjectMessage om, DIndexRequest request) {
        throw new UnsupportedOperationException();
    }

	public void onMessage(ObjectMessage message, InsertIMToPM request) {
		// TODO Auto-generated method stub

	}

	public void onMessage(ObjectMessage message, DeleteIMToPM request) {
		// TODO Auto-generated method stub

	}

	public void onMessage(ObjectMessage message, DIndexRelocationRequest request) {
		// TODO Auto-generated method stub

	}

	public void onMessage(ObjectMessage message, DIndexForwardRequest request) {
		// TODO Auto-generated method stub

	}

	public void onMessage(ObjectMessage message, MoveIMToPM request) {
		// TODO Auto-generated method stub

	}

	public void onMessage(ObjectMessage message, MovePMToSM request) {
		// TODO Auto-generated method stub

	}

	public ObjectMessage exchange(DIndexRequest request, long timeout) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
