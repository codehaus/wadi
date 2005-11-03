package org.codehaus.wadi.dindex;

import org.activecluster.Node;
import org.codehaus.wadi.dindex.impl.BucketFacade;
import org.codehaus.wadi.dindex.impl.BucketKeys;

/**
 * A PartitionManager is responsible for unambiguously renegotiating Partition ownership every time
 * that there is a change in Cluster membership and exposing these Partitions to the rest of the program,
 * whether local or Remote.
 * 
 * A lot of work needed here...
 * 
 * @author jules
 *
 */
public interface PartitionManager {

	BucketFacade getPartition(int bucket);
	BucketKeys getPartitionKeys();
	
	void init(DIndexConfig config);
	void start() throws Exception;
	void stop() throws Exception;
	
	void update(Node node);
	void regenerateMissingPartitions(Node[] living, Node[] leaving);
	void localise();
	void dequeue();
	
}
