package org.codehaus.wadi.sandbox.gridstate.messages;

import java.io.Serializable;

import javax.jms.Destination;

public class GetBOToSO implements Serializable {

	protected Serializable _key;
	protected Destination _po;
	protected Destination _bo;
	protected String _poCorrelationId;
	
	public GetBOToSO(Serializable key, Destination po, Destination bo, String poCorrelationId) {
			_key=key;
			_po=po;
			_bo=po;
			_poCorrelationId=poCorrelationId;
	}
	
	protected GetBOToSO() {
		// for deserialisation...
	}
	
	public Serializable getKey() {
		return _key;
	}

	public Destination getPO() {
		return _po;
	}

	public Destination getBO() {
		return _bo;
	}
	
	public String getPOCorrelationId() {
		return _poCorrelationId;
	}

}
