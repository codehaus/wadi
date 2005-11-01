package org.codehaus.wadi.impl;

public class SleepingReplicaterFactory extends StatelessReplicaterFactory {

	public SleepingReplicaterFactory(long delay) {
		super(new SleepingReplicater(delay));
	}
	
}
