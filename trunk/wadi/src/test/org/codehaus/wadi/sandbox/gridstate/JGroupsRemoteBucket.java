package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.Map;

import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Address;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;

public class JGroupsRemoteBucket implements BucketInterface {
	
	protected final Log _log=LogFactory.getLog(getClass().getName());
	protected final Address _address;
	
	public JGroupsRemoteBucket(Address address) {
		_address=address;
	}
	
	protected BucketConfig _config;
	
	public void init(BucketConfig config) {
		_config=config;
	}
	
	public Address getAddress() {
		return _address;
	}
	
	public Destination getDestination() {
		throw new UnsupportedOperationException("Too ActiveCluster specific?");
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
