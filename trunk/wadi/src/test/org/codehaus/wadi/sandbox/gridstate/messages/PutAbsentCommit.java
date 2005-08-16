package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class PutAbsentCommit implements Serializable {

	protected Serializable _key;
	
	public PutAbsentCommit(Serializable key) {
		_key=key;
	}
	
	protected PutAbsentCommit() {
		// for deserialisation...
	}
	
	public Serializable getKey() {
		return _key;
	}

}
