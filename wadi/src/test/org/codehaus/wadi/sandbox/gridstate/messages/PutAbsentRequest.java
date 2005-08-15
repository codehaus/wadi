package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

import javax.jms.Destination;

public class PutAbsentRequest implements Serializable {

	protected Serializable _key;
	protected Destination _destination;
	
	public PutAbsentRequest(Serializable key, Destination destination) {
		_key=key;
		_destination=destination;
	}

	protected PutAbsentRequest() {
		// for deserialisation
	}
	
	public Serializable getKey() {
		return _key;
	}
	
	public Destination getDestination() {
		return _destination;
	}

}
