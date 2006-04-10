package org.codehaus.wadi.activecluster;

import org.codehaus.wadi.test.AbstractTestRelocation;

public class TestRelocation extends AbstractTestRelocation {

	public TestRelocation(String arg0) {
		super(arg0);
	}
	
	public void testSessionRelocation() throws Exception {
		String clusterName="TEST";
		long timeout=5000;
		String clusterUri;
		clusterUri="vm://localhost";
		testSessionRelocation(new ActiveClusterDispatcher(clusterName, "red", clusterUri, timeout), new ActiveClusterDispatcher(clusterName, "green", clusterUri, timeout));
		clusterUri="peer://org.codehaus.wadi";
		testSessionRelocation(new ActiveClusterDispatcher(clusterName, "red", clusterUri, timeout), new ActiveClusterDispatcher(clusterName, "green", clusterUri, timeout));
	}
	
}
