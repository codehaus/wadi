package org.codehaus.wadi.sandbox.gridstate;

import java.util.Map;

import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Address;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;

public class ActiveClusterRemoteBucket implements BucketInterface {
	
	protected final Log _log=LogFactory.getLog(getClass().getName());
	protected final Destination _destination;
	
	public ActiveClusterRemoteBucket(Destination destination) {
		_destination=destination;
	}
	
	protected BucketConfig _config;
	
	public void init(BucketConfig config) {
		_config=config;
	}
	
	public Destination getDestination() {
		return _destination;
	}

	public Address getAddress() {
		throw new UnsupportedOperationException("What should we do here?");
	}
	
	public Location getLocation(Object key) {
		throw new UnsupportedOperationException("What should we do here?");
	}
	
	public ReadWriteLock getLock() {
		throw new UnsupportedOperationException("What should we do here?");
	}
	
	public Map getMap() {
		throw new UnsupportedOperationException("What should we do here?");
	}

}