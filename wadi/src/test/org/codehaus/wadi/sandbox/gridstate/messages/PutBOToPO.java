package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class PutBOToPO implements Serializable {

	protected boolean _success;
	
	public PutBOToPO(boolean success) {
		_success=success;
	}
	
	protected PutBOToPO() {
		// for deserialisation...
	}
	
	public boolean getSuccess() {
		return _success;
	}
	
}
