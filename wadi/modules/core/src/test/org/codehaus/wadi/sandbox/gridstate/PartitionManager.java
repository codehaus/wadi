package org.codehaus.wadi.sandbox.gridstate;

public interface PartitionManager {

	void init(PartitionConfig config);
	Partition[] getPartitions();
	
}
