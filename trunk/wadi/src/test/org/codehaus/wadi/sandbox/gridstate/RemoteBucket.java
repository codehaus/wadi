package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.Map;

import javax.jms.Destination;

public class RemoteBucket implements BucketInterface {

	public void init(BucketConfig config) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
		
	}

	public Destination putAbsent(Serializable key, Destination destination) {
		throw new UnsupportedOperationException("NYI");
	}

	public Destination putExists(Serializable key, Destination destination) {
		throw new UnsupportedOperationException("NYI");
	}

	public Destination getDestination() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("NYI");
	}
	
	public Serializable removeReturn(Serializable key, Map map) {
		throw new UnsupportedOperationException("NYI");
	}
	
	public void removeNoReturn(Serializable key) {
		throw new UnsupportedOperationException("NYI");
	}
}
