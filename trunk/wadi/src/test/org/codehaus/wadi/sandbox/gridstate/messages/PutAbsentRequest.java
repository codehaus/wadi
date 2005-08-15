package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class PutAbsentRequest implements Serializable {

	protected Serializable _key;
	
	public PutAbsentRequest(Serializable key) {
		_key=key;
	}

	protected PutAbsentRequest() {
		// for deserialisation
	}
	
	public Serializable getKey() {
		return _key;
	}

}
