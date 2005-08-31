package org.codehaus.wadi.sandbox.gridstate;

import javax.jms.Destination;

public class ActiveClusterLocation extends AbstractLocation {

	protected Destination _destination;
	
	public ActiveClusterLocation(Destination destination) {
		super();
		_destination=destination;
	}
	
	public Object getValue() {
		return _destination;
	}
	
	public void setValue(Object destination) {
		_destination=(Destination)destination;
	}
}
