package org.codehaus.wadi.impl;

import org.codehaus.wadi.ReplicableSessionConfig;

// A Session from which we can generate replication deltas, instead of complete backup copies...

// I think this approach is only possible under the assumption of ByValue Semantics.

public class PartiallyReplicableSession extends AbstractReplicableSession {

	public PartiallyReplicableSession(ReplicableSessionConfig config) {
		super(config);
		// NYI
	}
	
	public void readEnded() {
		throw new UnsupportedOperationException("NYI");
	}

}
