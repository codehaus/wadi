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
import org.codehaus.wadi.dindex.messages.DIndexDeletionRequest;
import org.codehaus.wadi.dindex.messages.DIndexForwardRequest;
import org.codehaus.wadi.dindex.messages.DIndexInsertionRequest;
import org.codehaus.wadi.dindex.messages.DIndexRelocationRequest;
import org.codehaus.wadi.dindex.newmessages.RelocationRequestI2P;

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

	public void onMessage(ObjectMessage message, DIndexInsertionRequest request) {
		// TODO Auto-generated method stub
		
	}

	public void onMessage(ObjectMessage message, DIndexDeletionRequest request) {
		// TODO Auto-generated method stub
		
	}

	public void onMessage(ObjectMessage message, DIndexRelocationRequest request) {
		// TODO Auto-generated method stub
		
	}

	public void onMessage(ObjectMessage message, DIndexForwardRequest request) {
		// TODO Auto-generated method stub
		
	}

	public void onMessage(ObjectMessage message, RelocationRequestI2P request) {
		// TODO Auto-generated method stub
		
	}

}
