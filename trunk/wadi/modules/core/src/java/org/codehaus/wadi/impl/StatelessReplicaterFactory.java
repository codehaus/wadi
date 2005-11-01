package org.codehaus.wadi.impl;

import org.codehaus.wadi.Replicater;
import org.codehaus.wadi.ReplicaterFactory;

public class StatelessReplicaterFactory implements ReplicaterFactory {

	protected Replicater _replicater;
	
	public StatelessReplicaterFactory(Replicater replicater) {
		_replicater=replicater;
	}
	
	public Replicater create() {
		return _replicater;
	}

}
