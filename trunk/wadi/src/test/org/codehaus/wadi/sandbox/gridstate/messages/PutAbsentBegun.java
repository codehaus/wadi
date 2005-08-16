package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class PutAbsentBegun implements Serializable {

	protected boolean _success;
	
	public PutAbsentBegun(boolean success) {
		_success=success;
	}
	
	protected PutAbsentBegun() {
		// for deserialisation...
	}
	
	public boolean getSuccess() {
		return _success;
	}
	
}
