package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.sandbox.gridstate.messages.PutAbsentRequest;
import org.codehaus.wadi.sandbox.gridstate.messages.PutAbsentResponse;

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

	public boolean putAbsent(Serializable key, Destination destination) {
		ObjectMessage message=_config.getDispatcher().exchangeSend(_config.getLocalNode().getDestination(), _destination, new PutAbsentRequest(key, destination), 2000L); // TODO - should retry
		try {
			return ((PutAbsentResponse)message.getObject()).getSuccess();
		} catch (JMSException e) {
			_log.error("unexpected problem", e); // TODO - lose when we start looping...
			return false; // FIXME
		}
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
