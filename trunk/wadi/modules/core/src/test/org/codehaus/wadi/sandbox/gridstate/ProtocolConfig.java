package org.codehaus.wadi.sandbox.gridstate;

import java.util.Map;


interface ProtocolConfig {

	public PartitionMapper getPartitionMapper();
	public Map getMap();
	public LockManager getPMSyncs();
	public LockManager getSMSyncs();

}
