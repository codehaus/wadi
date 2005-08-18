package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

import javax.jms.Destination;

public class RemovePOToBO implements Serializable {

	protected Serializable _key;
	protected boolean _returnOldValue;
	protected Destination _po;
	
	public RemovePOToBO(Serializable key, boolean returnOldValue, Destination po) {
		_key=key;
		_returnOldValue=returnOldValue;
		_po=po;
	}
	
	protected RemovePOToBO() {
		// for deserialisation...
	}
	
	public Serializable getKey() {
		return _key;
	}
	
	public boolean getReturnOldValue() {
		return _returnOldValue;
	}
	
	public Destination getPO() {
		return _po;
	}
	
}
