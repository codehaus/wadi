package org.codehaus.wadi.sandbox.gridstate;

import java.io.Serializable;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;

public class AbstractLocation implements Location, Serializable {

	protected transient ReadWriteLock _lock;
	protected transient boolean _invalid;

	public AbstractLocation() {
		_lock=new ReaderPreferenceReadWriteLock();
	}

	public ReadWriteLock getLock() {
		return _lock;
	}

	public void invalidate() {
		_invalid=true;
	}

}
