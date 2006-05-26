package org.codehaus.wadi.jgroups;

import org.codehaus.wadi.jgroups.JGroupsDispatcher;
import org.codehaus.wadi.relocation.AbstractTestRelocation;


public class TestJGRelocation extends AbstractTestRelocation {
	
	public TestJGRelocation(String name) {
		super(name);
	}
	
	public void testSessionRelocation() throws Exception {
        String clusterName="org.codehaus.wadi.TEST-"+Math.random();
        long timeout=5000;
		testSessionRelocation(new JGroupsDispatcher("red", clusterName, timeout, "default.xml"), new JGroupsDispatcher("green", clusterName, timeout, "default-minimalthreads.xml"));
	}
	
}
