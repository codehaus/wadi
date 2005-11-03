package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class ReadPOToBO implements Serializable {

	protected Object _key;
	protected Object _po;
	
	public ReadPOToBO(Object key, Object po) {
		_key=key;
		_po=po;
	}
	
	protected ReadPOToBO() {
		// for deserialisation ...
	}
	
	public Object getKey() {
		return _key;
	}
	
	public Object getPO() {
		return _po;
	}

}
