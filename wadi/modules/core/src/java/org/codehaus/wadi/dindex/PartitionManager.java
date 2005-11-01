package org.codehaus.wadi.dindex;

import org.codehaus.wadi.dindex.impl.BucketFacade;
import org.codehaus.wadi.dindex.impl.BucketKeys;

public interface PartitionManager {

	BucketFacade getBucket(int bucket);
	BucketKeys getBucketKeys();
	
}
