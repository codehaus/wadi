package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class WritePMToIM implements Serializable {

	protected boolean _success;
	
	public WritePMToIM(boolean success) {
		_success=success;
	}
	
	protected WritePMToIM() {
		// for deserialisation...
	}
	
	public boolean getSuccess() {
		return _success;
	}
	
}
