package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.Map;

import javax.jms.Destination;

public class Bucket implements BucketInterface {

	protected final BucketInterface _bucket;
	
	Bucket(BucketInterface bucket) {
		_bucket=bucket;
	}
	
	public void init(BucketConfig config) {
		_bucket.init(config);
	}
	
	public boolean putAbsent(Serializable key, Destination location) {
		return _bucket.putAbsent(key, location);
	}

	public Destination putExists(Serializable key, Destination location) {
		return _bucket.putExists(key, location);
	}
	
	public Serializable removeReturn(Serializable key, Map map) {
		return _bucket.removeReturn(key, map);
	}

	public void removeNoReturn(Serializable key) {
		_bucket.removeNoReturn(key);
	}

}
