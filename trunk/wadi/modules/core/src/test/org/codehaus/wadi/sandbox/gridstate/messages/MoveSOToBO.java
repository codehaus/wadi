package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

import org.omg.PortableInterceptor.SUCCESSFUL;

public class MoveSOToBO implements Serializable {

	boolean _success;
	
	public MoveSOToBO(boolean success) {
		_success=success;
	}
	
	public MoveSOToBO() {
		this(true);
	}
	
	public boolean getSuccess() {
		return _success;
	}
}
