package org.codehaus.wadi.sandbox.gridstate;


public interface BucketMapper {

	/**
	 * Given a key, map it to the corresponding Bucket index.
	 * 
	 * @param key
	 * @return
	 */
	int map(Object key);
	
}
