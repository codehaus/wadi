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
