package org.codehaus.wadi.test.relocation;

import org.codehaus.wadi.jgroups.JGroupsDispatcher;


public class TestJGRelocation extends AbstractTestRelocation {
	
	public TestJGRelocation(String arg0) {
		super(arg0);
	}
	
	public void testSessionRelocation() throws Exception {
		String clusterName="TEST";
		long timeout=5000;
		testSessionRelocation(new JGroupsDispatcher("red", clusterName, timeout), new JGroupsDispatcher("green", clusterName, timeout));
	}
	
}
