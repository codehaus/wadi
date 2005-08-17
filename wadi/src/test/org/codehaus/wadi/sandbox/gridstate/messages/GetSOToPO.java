package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class GetSOToPO implements Serializable {

	protected Serializable _value;
	
	public GetSOToPO(Serializable value) {
		_value=value;
	}
	
	protected GetSOToPO() {
		// for deserialisation...
	}
	
	public Serializable getValue() {
		return _value;
	}
	
}
