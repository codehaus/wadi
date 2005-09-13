package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class WriteBOToPO implements Serializable {

	protected boolean _success;
	
	public WriteBOToPO(boolean success) {
		_success=success;
	}
	
	protected WriteBOToPO() {
		// for deserialisation...
	}
	
	public boolean getSuccess() {
		return _success;
	}
	
}
