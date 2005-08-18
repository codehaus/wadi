package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.Map;

import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;

public class RemoteBucket implements BucketInterface {
	
	protected final Log _log=LogFactory.getLog(getClass().getName());
	protected final Destination _destination;
	
	public RemoteBucket(Destination destination) {
		_destination=destination;
	}
	
	protected BucketConfig _config;
	
	public void init(BucketConfig config) {
		_config=config;
	}
	
	public Destination getDestination() {
		return _destination;
	}
	
	public Location getLocation(Serializable key) {
		throw new UnsupportedOperationException("What should we do here?");
	}
	
	public ReadWriteLock getLock() {
		throw new UnsupportedOperationException("What should we do here?");
	}
	
	public Map getMap() {
		throw new UnsupportedOperationException("What should we do here?");
	}

}
