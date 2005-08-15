package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class PutAbsentResponse implements Serializable {

	protected boolean _success;
	
	public PutAbsentResponse(boolean success) {
		_success=success;
	}
	
	protected PutAbsentResponse() {
		// for deserialisation...
	}
	
	public boolean getSuccess() {
		return _success;
	}
	
}
