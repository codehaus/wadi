package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class WritePOToBO implements Serializable {

	protected Object _key;
	protected boolean _valueIsNull;
	protected boolean _overwrite;
	protected boolean _returnOldValue;
	protected Object _po;
	
	public WritePOToBO(Object key, boolean valueIsNull, boolean overwrite, boolean returnOldValue, Object po) {
		_key=key;
		_valueIsNull=valueIsNull;
		_overwrite=overwrite;
		_returnOldValue=returnOldValue;
		_po=po;
	}
	
	protected WritePOToBO() {
		// for deserialisation...
	}
	
	public Object getKey() {
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
	
	public Object getPO() {
		return _po;
	}
	
}
