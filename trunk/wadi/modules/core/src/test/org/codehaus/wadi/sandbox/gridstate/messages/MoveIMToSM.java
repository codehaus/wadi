package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class MoveIMToSM implements Serializable {

	protected boolean _success;
	
	public MoveIMToSM(boolean success) {
		_success=success;
	}
	
	public MoveIMToSM() {
		this(true);
	}
	
	public boolean getSuccess() {
		return _success;
	}
}
