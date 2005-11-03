package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class ReadIMToPM implements Serializable {

	protected Object _key;
	protected Object _im;
	
	public ReadIMToPM(Object key, Object im) {
		_key=key;
		_im=im;
	}
	
	protected ReadIMToPM() {
		// for deserialisation ...
	}
	
	public Object getKey() {
		return _key;
	}
	
	public Object getIM() {
		return _im;
	}

}
