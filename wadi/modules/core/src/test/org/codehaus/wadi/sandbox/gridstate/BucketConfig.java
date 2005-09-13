package org.codehaus.wadi.sandbox.gridstate;

import javax.jms.Destination;

import org.activecluster.LocalNode;
import org.codehaus.wadi.impl.Dispatcher;
import org.jgroups.Address;

public interface BucketConfig {

	Destination getLocalDestination();
	Address getLocalAddress();
	
	Dispatcher getDispatcher();
	
}
