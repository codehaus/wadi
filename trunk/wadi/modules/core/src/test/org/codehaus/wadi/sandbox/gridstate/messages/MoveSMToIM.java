package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class MoveSMToIM implements Serializable {

	protected Object _key;
	protected Object _value;
	
	public MoveSMToIM(Object key, Object value) {
		_key=key;
		_value=value;
	}
	
	protected MoveSMToIM() {
		// for deserialisation...
	}
	
	public Object getKey() {
		return _key;
	}

	public Object getValue() {
		return _value;
	}

}
