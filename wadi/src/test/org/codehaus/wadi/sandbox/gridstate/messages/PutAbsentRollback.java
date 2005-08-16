package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class PutAbsentRollback implements Serializable {

	protected Serializable _key;
	
	public PutAbsentRollback(Serializable key) {
		_key=key;
	}
	
	protected PutAbsentRollback() {
		// for deserialisation...
	}
	
	public Serializable getKey() {
		return _key;
	}

}
