package org.codehaus.wadi.impl;

import org.codehaus.wadi.Replicater;
import org.codehaus.wadi.ReplicaterFactory;

/**
 * MemoryReplicaters hold per Session state (the location of their replication partners), so we need to create a new
 * MemoryReplicater for each session.
 * 
 * @author jules
 *
 */
public class MemoryReplicaterFactory implements ReplicaterFactory {

	protected int _numReplicants;
	
	public MemoryReplicaterFactory(int numReplicants) {
		_numReplicants=numReplicants;
	}
	
	public Replicater create() {
		return new MemoryReplicater(_numReplicants);
	}

}
