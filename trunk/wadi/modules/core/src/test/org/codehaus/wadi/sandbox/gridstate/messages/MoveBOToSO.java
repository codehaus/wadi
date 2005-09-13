package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

import javax.jms.Destination;

public class MoveBOToSO implements Serializable {

	protected Object _key;
	protected Object _po;
	protected Object _bo;
	protected String _poCorrelationId;
	
	public MoveBOToSO(Object key, Object po, Object bo, String poCorrelationId) {
			_key=key;
			_po=po;
			_bo=po;
			_poCorrelationId=poCorrelationId;
	}
	
	protected MoveBOToSO() {
		// for deserialisation...
	}
	
	public Object getKey() {
		return _key;
	}

	public Object getPO() {
		return _po;
	}

	public Object getBO() {
		return _bo;
	}
	
	public String getPOCorrelationId() {
		return _poCorrelationId;
	}

}
