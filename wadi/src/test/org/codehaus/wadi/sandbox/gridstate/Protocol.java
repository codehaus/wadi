package org.codehaus.wadi.sandbox.gridstate;


public interface Protocol {

	void init(ProtocolConfig config);
	
	// called on PO...
	Object get(Object key);

	// called on PO...
	Object put(Object key, Object value, boolean overwrite, boolean returnOldValue);

	Object remove(Object key, boolean returnOldValue);
	
	Bucket[] getBuckets();
	
	BucketInterface createRemoteBucket();
	
    void start() throws Exception;
    
    void stop() throws Exception;

}