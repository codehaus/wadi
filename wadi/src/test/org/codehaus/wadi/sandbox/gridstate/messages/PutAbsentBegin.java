package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

import javax.jms.Destination;

public class PutAbsentBegin implements Serializable {

	protected Serializable _key;
	protected Destination _destination;
	
	public PutAbsentBegin(Serializable key, Destination destination) {
		_key=key;
		_destination=destination;
	}

	protected PutAbsentBegin() {
		// for deserialisation
	}
	
	public Serializable getKey() {
		return _key;
	}
	
	public Destination getDestination() {
		return _destination;
	}

}
