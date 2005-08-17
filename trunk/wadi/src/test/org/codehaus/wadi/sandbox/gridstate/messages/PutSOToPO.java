package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class PutSOToPO implements Serializable {

	protected Serializable _value;
	
	public PutSOToPO(Serializable value) {
		_value=value;
	}
	
	protected PutSOToPO() {
		// for deserialisation...
	}
	
	public Serializable getValue() {
		return _value;
	}
	
}
