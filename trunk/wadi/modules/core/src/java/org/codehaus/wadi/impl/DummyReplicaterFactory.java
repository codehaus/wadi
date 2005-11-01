package org.codehaus.wadi.impl;

public class DummyReplicaterFactory extends StatelessReplicaterFactory {

	public DummyReplicaterFactory() {
		super(new DummyReplicater());
	}
	
}
