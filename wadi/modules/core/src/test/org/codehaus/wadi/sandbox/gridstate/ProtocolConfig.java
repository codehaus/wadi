package org.codehaus.wadi.sandbox.gridstate;

import java.util.Map;


interface ProtocolConfig {
	
	public BucketMapper getBucketMapper();
	public Map getMap();
	public LockManager getBOSyncs();
	public LockManager getSOSyncs();
	
}
