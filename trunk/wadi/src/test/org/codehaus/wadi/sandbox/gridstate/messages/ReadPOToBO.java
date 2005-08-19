package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

import javax.jms.Destination;

public class ReadPOToBO implements Serializable {

	protected Serializable _key;
	protected Destination _po;
	
	public ReadPOToBO(Serializable key, Destination po) {
		_key=key;
		_po=po;
	}
	
	protected ReadPOToBO() {
		// for deserialisation ...
	}
	
	public Serializable getKey() {
		return _key;
	}
	
	public Destination getPO() {
		return _po;
	}
}
