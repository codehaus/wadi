package org.codehaus.wadi.impl;

import org.codehaus.wadi.Replicater;

public class SleepingReplicater implements Replicater {

	protected final long _delay;
	
	public SleepingReplicater(long delay) {
		_delay=delay;
	}
	
	public void create(Object tmp) {
		try {
			Thread.sleep(_delay);
		} catch (InterruptedException e) {
			// do nothing
		}
	}

	public void update(Object tmp) {
		try {
			Thread.sleep(_delay);
		} catch (InterruptedException e) {
			// do nothing
		}
	}

	public void destroy(Object tmp) {
		try {
			Thread.sleep(_delay);
		} catch (InterruptedException e) {
			// do nothing
		}
	}

}
