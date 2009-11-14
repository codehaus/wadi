package org.codehaus.wadi.jgroups;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.jgroups.JGroupsDispatcher;
import org.codehaus.wadi.relocation.AbstractTestRelocation;


public class TestJGRelocation extends AbstractTestRelocation {
    private final String clusterName = JGTestUtil.TEST_CLUSTER_NAME;
	
    protected Dispatcher newDispatcher(String name) throws Exception {
        return new JGroupsDispatcher(clusterName, name, null, JGTestUtil.TEST_CLUSTER_CONFIG);
    }
    
}
