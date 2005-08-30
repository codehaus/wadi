package org.codehaus.wadi.sandbox.gridstate;

import javax.jms.Destination;

public class ActiveClusterLocation extends AbstractLocation {

	protected Destination _destination;
	
	public ActiveClusterLocation(Destination destination) {
		super();
		_destination=destination;
	}
	
	public Destination getDestination() {
		return _destination;
	}
	
	public void setDestination(Destination destination) {
		_destination=destination;
	}
}
