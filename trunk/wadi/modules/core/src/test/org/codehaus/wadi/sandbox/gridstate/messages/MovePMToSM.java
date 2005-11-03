package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

public class MovePMToSM implements Serializable {

	protected Object _key;
	protected Object _im;
	protected Object _pm;
	protected String _imCorrelationId;
	
	public MovePMToSM(Object key, Object im, Object pm, String imCorrelationId) {
			_key=key;
			_im=im;
			_pm=im;
			_imCorrelationId=imCorrelationId;
	}
	
	protected MovePMToSM() {
		// for deserialisation...
	}
	
	public Object getKey() {
		return _key;
	}

	public Object getIM() {
		return _im;
	}

	public Object getPM() {
		return _pm;
	}
	
	public String getIMCorrelationId() {
		return _imCorrelationId;
	}

}
