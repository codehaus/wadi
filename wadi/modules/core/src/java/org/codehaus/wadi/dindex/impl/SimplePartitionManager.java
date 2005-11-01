package org.codehaus.wadi.dindex.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.dindex.BucketConfig;
import org.codehaus.wadi.dindex.PartitionManager;

public class SimplePartitionManager implements PartitionManager {

    protected final String _nodeName;
    protected final Log _log;
    protected final int _numPartitions;
    protected final BucketFacade[] _partitions;
    
    public SimplePartitionManager(String nodeName, int numPartitions, BucketConfig config) {
    	_nodeName=nodeName;
    	_log=LogFactory.getLog(getClass().getName()+"#"+_nodeName);
    	_numPartitions=numPartitions;

        _partitions=new BucketFacade[_numPartitions];
        long timeStamp=System.currentTimeMillis();
        boolean queueing=true;
        for (int i=0; i<_numPartitions; i++)
            _partitions[i]=new BucketFacade(i, timeStamp, new DummyBucket(i), queueing, config);
    }

	public BucketFacade getBucket(int bucket) {
		return _partitions[bucket];
	}
	
	public BucketKeys getBucketKeys() {
		 return new BucketKeys(_partitions);
	}
    
}
