package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class MoveSOToPO implements Serializable {

	protected Serializable _value;
	
	public MoveSOToPO(Serializable value) {
		_value=value;
	}
	
	protected MoveSOToPO() {
		// for deserialisation...
	}
	
	public Serializable getValue() {
		return _value;
	}
	
}
