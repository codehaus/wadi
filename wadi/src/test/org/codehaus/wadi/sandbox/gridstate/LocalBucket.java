package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;

public class LocalBucket implements BucketInterface {
	
	protected final Map _map=new HashMap();

	protected BucketConfig _config;

	public void init(BucketConfig config) {
		_config=config;
	}
	
	public boolean putAbsent(Serializable key, Destination destination) { // consider optimisations...
		synchronized (_map) {
			Location location=(Location)_map.get(key);
			if (location==null) {
				_map.put(key, new Location(destination));
				return true;
			} else {
				return false;
			}
		}
	}

	public Destination putExists(Serializable key, Destination destination) {
		Location location;
		synchronized (_map) {
			location=(Location)_map.put(key, new Location(destination));
		}
		return location==null?null:location.getDestination();
	}

	public Serializable removeReturn(Serializable key, Map data) {
		synchronized (_map) {
			Location location=(Location)_map.remove(key);
			if (location!=null && location.getDestination()==_config.getLocalNode().getDestination()) { // i.e. Data and Bucket present in same vm...
				synchronized (data) {
					return (Serializable)data.remove(key);
				}
			} else {
				throw new UnsupportedOperationException("NYI");
				// we will have to retrieve the data from another vm...
				// we can avoid this is the request came from the vm that currently holds the data
			}
		}
	}
	
	public void removeNoReturn(Serializable key) {
		synchronized (_map) {
			_map.remove(key);
		}
	}
	
}
