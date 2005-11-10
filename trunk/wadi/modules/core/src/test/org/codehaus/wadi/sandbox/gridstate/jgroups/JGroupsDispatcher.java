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
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.DispatcherConfig;
import org.codehaus.wadi.JGroupsDispatcherConfig;
import org.codehaus.wadi.impl.AbstractDispatcher;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.Message;
import org.jgroups.MessageListener;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;

public class JGroupsDispatcher extends AbstractDispatcher implements RequestHandler, MessageListener {

	protected final Log _log=LogFactory.getLog(getClass().getName());

	public JGroupsDispatcher() {
		super();
	}

	protected Channel _channel;
	protected MessageDispatcher _dispatcher;
	protected Destination _localDestination;

	public void init(DispatcherConfig config) throws Exception {
		super.init(config);
		_channel=((JGroupsDispatcherConfig)_config).getChannel();
		_dispatcher=new MessageDispatcher(_channel, this, null, this);
		_channel.connect("WADI"); // TODO - parameterise name
		_localDestination=new JGroupsDestination(_channel.getLocalAddress());
	}

	// AbstractDispatcher api

	public static class Envelope extends JGroupsObjectMessage {
		
		protected Address _replyTo;
		protected Address _destination;
		protected String _outgoingCorrelationId;
		protected String _incomingCorrelationId;
		
		protected Serializable _letter;
		
		// ObjectMessage
		public Serializable getObject() throws JMSException {
			return _letter;
		}
		
		public void setObject(Serializable letter) throws JMSException {
			_letter=letter;
		}
		
		public Destination getJMSReplyTo() throws JMSException {
			return new JGroupsDestination(_replyTo);
		}

		public void setJMSReplyTo(Destination destination) throws JMSException {
			_replyTo=((JGroupsDestination)destination).getAddress();
		}

		public Destination getJMSDestination() throws JMSException {
			return new JGroupsDestination(_destination);
		}
		
		
		public void setJMSDestination(Destination destination) throws JMSException {
			_destination=((JGroupsDestination)destination).getAddress();
		}
	}
	
	public void send(Destination to, ObjectMessage message) throws Exception {
		//_log.info("JGROUPS MESSAGE SENT: "+((Envelope)message)._letter+" -> "+((JGroupsDestination)to).getAddress());
		_channel.send(((JGroupsDestination)to).getAddress(), _channel.getLocalAddress(), (Envelope)message);
	}

	public ObjectMessage createObjectMessage() {
		return new Envelope();
	}

	// RequestHandler api (JGroups)

	public Object handle(Message msg) {
		_log.info("JGROUPS MESSAGE: "+msg);
		return null;
	}

	// AbstractDispatcher API

    public String getIncomingCorrelationId(ObjectMessage message) throws Exception {
    	return ((Envelope)message)._incomingCorrelationId;
    }

    public void setIncomingCorrelationId(ObjectMessage message, String correlationId) throws Exception {
    	((Envelope)message)._incomingCorrelationId=correlationId;
    }

    public String getOutgoingCorrelationId(ObjectMessage message) throws Exception {
    	return ((Envelope)message)._outgoingCorrelationId;
    }

    public void setOutgoingCorrelationId(ObjectMessage message, String correlationId) throws Exception {
    	((Envelope)message)._outgoingCorrelationId=correlationId;
    }

    public Destination getLocalDestination() {
		return _localDestination;
	}

	public void setDistributedState(Map state) throws Exception {
		_log.warn("setDistributedState - NYI");
		//throw new UnsupportedOperationException("NYI");
	}

	public void start() throws Exception {
		_dispatcher.start();
	}

	public void stop() throws Exception {
		_dispatcher.stop();
	}

	public String getNodeName(Destination destination) {
		//_log.warn("getNodeName- NYI");
		//throw new UnsupportedOperationException("NYI");
		return "<unknown>";
	}
	
	// MessageListener API

	public void receive(Message msg) {
		Envelope envelope=(Envelope)msg.getObject();
		//_log.info("JGROUPS MESSAGE RECEIVED: "+envelope._letter);

		// an Envelope is an Object Message...
		
		onMessage(envelope);
		
	}

	public byte[] getState() {
		return null;
	}

	public void setState(byte[] state) {
	}


}
