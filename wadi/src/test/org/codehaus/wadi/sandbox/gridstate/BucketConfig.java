package org.codehaus.wadi.sandbox.gridstate;

import org.activecluster.LocalNode;
import org.codehaus.wadi.impl.Dispatcher;

public interface BucketConfig {

	LocalNode getLocalNode();
	Dispatcher getDispatcher();
	
}
