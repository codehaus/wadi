package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.gridstate.messages.PutAbsentCommit;
import org.codehaus.wadi.sandbox.gridstate.messages.PutAbsentBegin;
import org.codehaus.wadi.sandbox.gridstate.messages.PutAbsentBegun;
import org.codehaus.wadi.sandbox.gridstate.messages.PutAbsentRollback;

public class RemoteBucket implements BucketInterface {
	
	protected final Log _log=LogFactory.getLog(getClass().getName());
	protected final Destination _destination;
	
	public RemoteBucket(Destination destination) {
		_destination=destination;
	}
	
	protected BucketConfig _config;
	
	public void init(BucketConfig config) {
		_config=config;
	}

	public boolean putAbsentBegin(Conversation conversation, Serializable key, Destination destination) {
		ObjectMessage message=_config.getDispatcher().exchangeSendLoop(_config.getLocalNode().getDestination(), _destination, new PutAbsentBegin(key, destination), 2000L); // TODO - should retry
		conversation._state=message;
		try {
			return ((PutAbsentBegun)message.getObject()).getSuccess();
		} catch (JMSException e) {
			_log.error("unexpected problem", e); // TODO - lose when we start looping...
			return false; // FIXME
		}
	}
	
	public void putAbsentCommit(Conversation conversation, Serializable key, Destination destination) {
		_config.getDispatcher().reply((ObjectMessage)conversation._state, new PutAbsentCommit(key));
	}

	public void putAbsentRollback(Conversation conversation, Serializable key, Destination destination) {
		_config.getDispatcher().reply((ObjectMessage)conversation._state, new PutAbsentRollback(key));
	}

	public Destination putExists(Serializable key, Destination destination) {
		throw new UnsupportedOperationException("NYI");
	}

	public Serializable removeReturn(Serializable key, Map map) {
		throw new UnsupportedOperationException("NYI");
	}
	
	public void removeNoReturn(Serializable key) {
		throw new UnsupportedOperationException("NYI");
	}
}
