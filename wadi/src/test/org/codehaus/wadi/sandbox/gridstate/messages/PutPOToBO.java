package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

import javax.jms.Destination;

public class PutPOToBO implements Serializable {

	protected Serializable _key;
	protected boolean _valueIsNull;
	protected boolean _overwrite;
	protected boolean _returnOldValue;
	protected Destination _po;
	
	public PutPOToBO(Serializable key, boolean valueIsNull, boolean overwrite, boolean returnOldValue, Destination po) {
		_key=key;
		_valueIsNull=valueIsNull;
		_overwrite=overwrite;
		_returnOldValue=returnOldValue;
		_po=po;
	}
	
	protected PutPOToBO() {
		// for deserialisation...
	}
	
	public Serializable getKey() {
		return _key;
	}
	
	public boolean getValueIsNull() {
		return _valueIsNull;
	}
	
	public boolean getOverwrite() {
		return _overwrite;
	}
	
	public boolean getReturnOldValue() {
		return _returnOldValue;
	}
	
	public Destination getPO() {
		return _po;
	}
	
}
