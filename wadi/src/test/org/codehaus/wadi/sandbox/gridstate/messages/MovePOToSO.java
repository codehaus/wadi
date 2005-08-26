package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class MovePOToSO implements Serializable {

	protected boolean _success;
	
	public MovePOToSO(boolean success) {
		_success=success;
	}
	
	public MovePOToSO() {
		this(true);
	}
	
	public boolean getSuccess() {
		return _success;
	}
}
