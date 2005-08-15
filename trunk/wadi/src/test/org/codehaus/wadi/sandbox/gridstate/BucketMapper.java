package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;

public interface BucketMapper {

	/**
	 * Given a key, map it to the corresponding Bucket index.
	 * 
	 * @param key
	 * @return
	 */
	int map(Serializable key);
	
}
