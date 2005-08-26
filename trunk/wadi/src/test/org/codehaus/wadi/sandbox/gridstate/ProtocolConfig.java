package org.codehaus.wadi.sandbox.gridstate;

import java.util.Map;


interface ProtocolConfig {
	
	public BucketMapper getBucketMapper();
	public Map getMap();
	public SyncMap getBOSyncs();
	public SyncMap getSOSyncs();
	
}
