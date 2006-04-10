package org.codehaus.wadi.jgroups;

import org.codehaus.wadi.test.AbstractTestRelocation;

public class TestRelocation extends AbstractTestRelocation {
	
	public TestRelocation(String arg0) {
		super(arg0);
	}
	
	public void testSessionRelocation() throws Exception {
		String clusterName="TEST";
		long timeout=5000;
		testSessionRelocation(new JGroupsDispatcher("red", clusterName, timeout), new JGroupsDispatcher("green", clusterName, timeout));
	}
	
}
