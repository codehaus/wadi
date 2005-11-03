package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class MoveSMToPM implements Serializable {

	boolean _success;
	
	public MoveSMToPM(boolean success) {
		_success=success;
	}
	
	public MoveSMToPM() {
		this(true);
	}
	
	public boolean getSuccess() {
		return _success;
	}
}
