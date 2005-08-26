package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;
import java.util.Map;

import javax.jms.Destination;

import org.jgroups.Address;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;

public interface BucketInterface {

	void init(BucketConfig config);
	
	// Serializable executeSync(Object process);
	// void executeASync(Object process);
	
	// yeugh !! - TODO
	Destination getDestination();
	Address getAddress();
	
	Location getLocation(Serializable key);
	ReadWriteLock getLock();
	Map getMap();
	
}
