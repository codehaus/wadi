package org.codehaus.wadi.sandbox.gridstate;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;

public interface Location {

	ReadWriteLock getLock();

	void invalidate();
	
	Object getValue();
	void setValue(Object value);

}