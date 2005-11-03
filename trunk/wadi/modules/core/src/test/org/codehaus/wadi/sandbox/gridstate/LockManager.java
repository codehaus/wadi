package org.codehaus.wadi.sandbox.gridstate;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public interface LockManager {

	Sync acquire(Object key);

}