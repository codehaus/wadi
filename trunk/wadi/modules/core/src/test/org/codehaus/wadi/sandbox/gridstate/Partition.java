package org.codehaus.wadi.sandbox.gridstate;

import java.util.Map;

import javax.jms.Destination;

import org.jgroups.Address;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;

public class Partition implements PartitionInterface {

	protected final PartitionInterface _partition;
	
	Partition(PartitionInterface partition) {
		_partition=partition;
	}
	
	public void init(PartitionConfig config) {
		_partition.init(config);
	}
	
	public Destination getDestination() {
		return _partition.getDestination();
	}
	
	public Address getAddress() {
		return _partition.getAddress();
	}

	public Location getLocation(Object key) {
		return _partition.getLocation(key);
	}
	
	public ReadWriteLock getLock() {
		return _partition.getLock();
	}
	
	public Map getMap() {
		return _partition.getMap();
	}
	
}
