package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;

import javax.jms.Destination;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;

public class Location implements Serializable {

	protected transient ReadWriteLock _lock;
	protected Destination _destination;
	
	public Location(Destination destination) {
		this();
		_destination=destination;
	}
	
	protected Location() {
		_lock=new ReaderPreferenceReadWriteLock();
	}
	
	public Destination getDestination() {
		return _destination;
	}
	
}
