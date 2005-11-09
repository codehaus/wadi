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
package org.codehaus.wadi.sandbox.gridstate.jgroups;

import java.io.Serializable;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.DispatcherConfig;
import org.codehaus.wadi.JGroupsDispatcherConfig;
import org.codehaus.wadi.impl.AbstractDispatcher;
import org.jgroups.Channel;
import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;

public class JGroupsDispatcher extends AbstractDispatcher implements RequestHandler {

	protected final Log _log=LogFactory.getLog(getClass().getName());
	
	public JGroupsDispatcher() {
		super();
	}

	protected Channel _channel;
	protected MessageDispatcher _dispatcher;

	public void init(DispatcherConfig config) throws Exception {
		super.init(config);
		_channel=((JGroupsDispatcherConfig)_config).getChannel();
		_dispatcher=new MessageDispatcher(_channel, null, null, this);
		_dispatcher.start();
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
		_log.info("JGROUPS MESSAGE: "+msg);
		return null;
	}

	// AbstractDispatcher API

    public String getIncomingCorrelationId(ObjectMessage message) throws Exception {
    	throw new UnsupportedOperationException("NYI");
    }

	public Destination getLocalDestination() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setDistributedState(Map state) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void start() throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void stop() throws Exception {
		// TODO Auto-generated method stub
		
	}

	public String getNodeName(Destination destination) {
		// TODO Auto-generated method stub
		return null;
	}


}
