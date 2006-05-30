package org.codehaus.wadi.jgroups;

import org.codehaus.wadi.jgroups.JGroupsDispatcher;
import org.codehaus.wadi.relocation.AbstractTestRelocation;


public class TestJGRelocation extends AbstractTestRelocation {
	
	public TestJGRelocation(String name) {
		super(name);
	}
	
	public void testSessionRelocation() throws Exception {
        String clusterName=JGroupsCluster.TEST_CLUSTER_NAME;
        long timeout=5000;
		testSessionRelocation(new JGroupsDispatcher("red", clusterName, timeout, JGroupsCluster.TEST_CLUSTER_CONFIG), new JGroupsDispatcher("green", clusterName, timeout, JGroupsCluster.TEST_CLUSTER_CONFIG));
	}
	
}
