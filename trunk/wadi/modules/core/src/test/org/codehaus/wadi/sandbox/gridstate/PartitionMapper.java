package org.codehaus.wadi.sandbox.gridstate;


public interface PartitionMapper {

	/**
	 * Given a key, map it to the corresponding Partition index.
	 * 
	 * @param key
	 * @return
	 */
	int map(Object key);
	
}
