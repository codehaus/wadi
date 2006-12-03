package org.codehaus.wadi.tribes;

import org.codehaus.wadi.tribes.TribesDispatcher;
import org.codehaus.wadi.relocation.AbstractTestRelocation;


public class TestTribesRelocation extends AbstractTestRelocation {

	public void testSessionRelocation() throws Exception {
        String clusterName="dummy";
        long timeout=5000;
        testSessionRelocation(new TribesDispatcher(clusterName, "red", null, timeout, ""), new TribesDispatcher(clusterName, "green", null, timeout, ""));
	}

}
