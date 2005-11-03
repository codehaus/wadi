package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class WriteIMToPM implements Serializable {

	protected Object _key;
	protected boolean _valueIsNull;
	protected boolean _overwrite;
	protected boolean _returnOldValue;
	protected Object _im;
	
	public WriteIMToPM(Object key, boolean valueIsNull, boolean overwrite, boolean returnOldValue, Object im) {
		_key=key;
		_valueIsNull=valueIsNull;
		_overwrite=overwrite;
		_returnOldValue=returnOldValue;
		_im=im;
	}
	
	protected WriteIMToPM() {
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
	
	public Object getIM() {
		return _im;
	}
	
}
