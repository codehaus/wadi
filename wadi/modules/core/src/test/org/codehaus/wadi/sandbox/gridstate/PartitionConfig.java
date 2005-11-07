package org.codehaus.wadi.sandbox.gridstate;

import javax.jms.Destination;

import org.codehaus.wadi.Dispatcher;
import org.jgroups.Address;

public interface PartitionConfig {

	Destination getLocalDestination();
	Address getLocalAddress();
	
	Dispatcher getDispatcher();
	
}
