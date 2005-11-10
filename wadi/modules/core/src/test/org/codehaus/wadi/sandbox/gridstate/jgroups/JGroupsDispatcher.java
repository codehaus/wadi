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
import org.codehaus.wadi.impl.Quipu;
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
		_localDestination=new JGroupsDestination(_channel.getLocalAddress());
	}

	// AbstractDispatcher api

	public boolean send(Destination from, Destination to, String outgoingCorrelationId, Serializable body) {
		throw new UnsupportedOperationException("NYI");
		//return false;
	}

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
		_channel.send(((JGroupsDestination)to).getAddress(), _channel.getLocalAddress(), (Envelope)message);
	}
	
    /* (non-Javadoc)
	 * @see org.codehaus.wadi.impl.Dispatcher#exchangeSend(javax.jms.Destination, javax.jms.Destination, java.io.Serializable, long, java.lang.String)
	 */
    public ObjectMessage exchangeSend(Destination from, Destination to, Serializable body, long timeout, String targetCorrelationId) {
    	Envelope envelope=new Envelope();
    	envelope._replyTo=((JGroupsDestination)from).getAddress();
    	envelope._destination=((JGroupsDestination)to).getAddress();
    	envelope._letter=body;
		String correlationId=nextCorrelationId();
		envelope._outgoingCorrelationId=correlationId;
		if (targetCorrelationId!=null)
			envelope._incomingCorrelationId=targetCorrelationId;
    	try {
    		Quipu rv=setRendezVous(correlationId, 1);
    		_log.trace("exchangeSend {"+correlationId+"}: "+getNodeName(from)+" -> "+getNodeName(to)+" : "+body);
    		_channel.send(envelope._destination, envelope._replyTo, envelope);
    		return attemptRendezVous(correlationId, rv, timeout);
    	} catch (Exception e) {
    		_log.error("problem sending "+body, e);
    		return null;
    	}
    }
    
	public ObjectMessage exchangeSend(Destination from, Destination to, String outgoingCorrelationId, Serializable body, long timeout) {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public boolean reply(Destination from, Destination to, String incomingCorrelationId, Serializable body) {
		throw new UnsupportedOperationException("NYI");
		//return false;
	}

	public ObjectMessage exchangeReply(ObjectMessage message, Serializable body, long timeout) {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public ObjectMessage exchangeReplyLoop(ObjectMessage message, Serializable body, long timeout) {
		throw new UnsupportedOperationException("NYI");
		//return null;
	}

	public boolean forward(ObjectMessage message, Destination destination) {
		throw new UnsupportedOperationException("NYI");
		//return false;
	}

	public boolean forward(ObjectMessage message, Destination destination, Serializable body) {
		throw new UnsupportedOperationException("NYI");
		//return false;
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
		_channel.connect("WADI"); // TODO - parameterise name
		_dispatcher.start();
	}

	public void stop() throws Exception {
		throw new UnsupportedOperationException("NYI");
	}

	public String getNodeName(Destination destination) {
		_log.warn("getNodeName- NYI");
		//throw new UnsupportedOperationException("NYI");
		return "<unknown>";
	}
	
	// MessageListener API

	public void receive(Message msg) {
		Envelope envelope=(Envelope)msg.getObject();
		_log.info("JGROUPS MESSAGE RECEIVED: "+envelope);

		// an Envelope is an Object Message...
		
		onMessage(envelope);
		
	}

	public byte[] getState() {
		return null;
	}

	public void setState(byte[] state) {
	}


}
