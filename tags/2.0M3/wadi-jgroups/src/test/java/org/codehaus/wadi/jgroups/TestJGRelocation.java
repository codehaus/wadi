package org.codehaus.wadi.jgroups;

import org.codehaus.wadi.jgroups.JGroupsDispatcher;
import org.codehaus.wadi.relocation.AbstractTestRelocation;


public class TestJGRelocation extends AbstractTestRelocation {
	
	public void testSessionRelocation() throws Exception {
        String clusterName = JGTestUtil.TEST_CLUSTER_NAME;
        long timeout = 5000;
        testSessionRelocation(new JGroupsDispatcher(clusterName, "red", null, timeout, JGTestUtil.TEST_CLUSTER_CONFIG),
                new JGroupsDispatcher(clusterName, "green", null, timeout, JGTestUtil.TEST_CLUSTER_CONFIG));
    }
	
}
