package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.Map;

import javax.jms.Destination;

import org.jgroups.Address;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;

public class Bucket implements BucketInterface {

	protected final BucketInterface _bucket;
	
	Bucket(BucketInterface bucket) {
		_bucket=bucket;
	}
	
	public void init(BucketConfig config) {
		_bucket.init(config);
	}
	
	public Destination getDestination() {
		return _bucket.getDestination();
	}
	
	public Address getAddress() {
		return _bucket.getAddress();
	}

	public Location getLocation(Serializable key) {
		return _bucket.getLocation(key);
	}
	
	public ReadWriteLock getLock() {
		return _bucket.getLock();
	}
	
	public Map getMap() {
		return _bucket.getMap();
	}
	
}
