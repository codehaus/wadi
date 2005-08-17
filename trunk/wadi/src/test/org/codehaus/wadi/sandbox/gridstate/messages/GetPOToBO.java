package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

import javax.jms.Destination;

public class GetPOToBO implements Serializable {

	protected Serializable _key;
	protected Destination _po;
	
	public GetPOToBO(Serializable key, Destination po) {
		_key=key;
		_po=po;
	}
	
	protected GetPOToBO() {
		// for deserialisation ...
	}
	
	public Serializable getKey() {
		return _key;
	}
	
	public Destination getPO() {
		return _po;
	}
}
