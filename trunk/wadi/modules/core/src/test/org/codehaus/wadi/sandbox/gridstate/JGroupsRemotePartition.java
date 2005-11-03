package org.codehaus.wadi.sandbox.gridstate;

import java.util.Map;

import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jgroups.Address;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;

public class JGroupsRemotePartition implements PartitionInterface {
	
	protected final Log _log=LogFactory.getLog(getClass().getName());
	protected final Address _address;
	
	public JGroupsRemotePartition(Address address) {
		_address=address;
	}
	
	protected PartitionConfig _config;
	
	public void init(PartitionConfig config) {
		_config=config;
	}
	
	public Address getAddress() {
		return _address;
	}
	
	public Destination getDestination() {
		throw new UnsupportedOperationException("Too ActiveCluster specific?");
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
