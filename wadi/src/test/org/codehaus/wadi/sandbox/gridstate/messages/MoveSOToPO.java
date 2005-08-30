package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class MoveSOToPO implements Serializable {

	protected Object _key;
	protected Object _value;
	
	public MoveSOToPO(Object key, Object value) {
		_key=key;
		_value=value;
	}
	
	protected MoveSOToPO() {
		// for deserialisation...
	}
	
	public Object getKey() {
		return _key;
	}

	public Object getValue() {
		return _value;
	}

}
