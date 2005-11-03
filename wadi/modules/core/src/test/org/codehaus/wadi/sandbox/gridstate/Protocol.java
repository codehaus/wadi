package org.codehaus.wadi.sandbox.gridstate;


public interface Protocol {

	void init(ProtocolConfig config);

	// called on IM...
	Object get(Object key);

	// called on IM...
	Object put(Object key, Object value, boolean overwrite, boolean returnOldValue);

	Object remove(Object key, boolean returnOldValue);

	Partition[] getPartitions();

	PartitionInterface createRemotePartition();

    void start() throws Exception;

    void stop() throws Exception;

	Object syncRpc(Object address, String methodName, Object message) throws Exception;

	Object getLocalLocation();
}
