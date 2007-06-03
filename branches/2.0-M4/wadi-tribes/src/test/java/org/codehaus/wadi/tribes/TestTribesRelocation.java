package org.codehaus.wadi.tribes;

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.relocation.AbstractTestRelocation;


public class TestTribesRelocation extends AbstractTestRelocation {
    private String clusterName = "dummy";
    private long timeout = 5000;

    protected Dispatcher newDispatcher(String name) {
        return new TribesDispatcher(clusterName, name, null, "");
    }
    
}
