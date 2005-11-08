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
package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;

import javax.jms.Destination;
import javax.jms.ObjectMessage;

import org.codehaus.wadi.DispatcherConfig;
import org.codehaus.wadi.impl.AbstractDispatcher;
import org.jgroups.Message;
import org.jgroups.blocks.RequestHandler;

public class JGroupsDispatcher extends AbstractDispatcher implements RequestHandler {

	public JGroupsDispatcher() {
		super();
		// TODO Auto-generated constructor stub
	}

	public void init(DispatcherConfig config) throws Exception {
		super.init(config);
	}

	// AbstractDispatcher api

	public boolean send(Destination from, Destination to, String outgoingCorrelationId, Serializable body) {
		// TODO Auto-generated method stub
		return false;
	}

	public ObjectMessage exchangeSend(Destination from, Destination to, Serializable body, long timeout) {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectMessage exchangeSend(Destination from, Destination to, Serializable body, long timeout, String targetCorrelationId) {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectMessage exchangeSendLoop(Destination from, Destination to, Serializable body, long timeout, int iterations) {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectMessage exchangeSend(Destination from, Destination to, String outgoingCorrelationId, Serializable body, long timeout) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean reply(Destination from, Destination to, String incomingCorrelationId, Serializable body) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean reply(ObjectMessage message, Serializable body) {
		// TODO Auto-generated method stub
		return false;
	}

	public ObjectMessage exchangeReply(ObjectMessage message, Serializable body, long timeout) {
		// TODO Auto-generated method stub
		return null;
	}

	public ObjectMessage exchangeReplyLoop(ObjectMessage message, Serializable body, long timeout) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean forward(ObjectMessage message, Destination destination) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean forward(ObjectMessage message, Destination destination, Serializable body) {
		// TODO Auto-generated method stub
		return false;
	}

	// RequestHandler api (JGroups)

	public Object handle(Message msg) {
		// TODO Auto-generated method stub
		return null;
	}

	// AbstractDispatcher API

    public String getIncomingCorrelationId(ObjectMessage message) throws Exception {
    	throw new UnsupportedOperationException("NYI");
    }


}
