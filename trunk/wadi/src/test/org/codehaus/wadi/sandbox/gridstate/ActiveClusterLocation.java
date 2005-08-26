package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;

import javax.jms.Destination;

import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;

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
