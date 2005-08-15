package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class PutRequest implements Serializable {

	protected Serializable _key;
	protected boolean _overwrite;
	
	public PutRequest(Serializable key, boolean overwrite) {
		_key=key;
		_overwrite=overwrite;
	}

	protected PutRequest() {
		// for deserialisation
	}
	
	public Serializable getKey() {
		return _key;
	}
	
	public boolean getOverwrite() {
		return _overwrite;
	}
	
}
