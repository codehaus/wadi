/**
 * Copyright 2007 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.codehaus.wadi.aop.tracker.basic;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.annotation.ClusteredState;
import org.codehaus.wadi.aop.aspectj.ClusteredStateAspectUtil;
import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.aop.tracker.InstanceTrackerFactory;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class AbstractReplacerTest extends RMockTestCase {

    protected InstanceRegistry instanceRegistry;
    protected Set<InstanceTracker> trackers;
    protected ClusteredStateMarker stateMarker;
    protected InstanceTracker tracker;
    protected String instanceId;
    protected CompoundReplacer parentReplacer;

    @Override
    protected void setUp() throws Exception {
        tracker = (InstanceTracker) mock(InstanceTracker.class);
        InstanceTrackerFactory trackerFactory = new InstanceTrackerFactory() {
            public InstanceTracker newInstanceTracker(ClusteredStateMarker stateMarker) {
                return tracker;
            }    
        };
        ClusteredStateAspectUtil.resetInstanceTrackerFactory();
        ClusteredStateAspectUtil.setInstanceTrackerFactory(trackerFactory);
        
        trackers = new HashSet<InstanceTracker>();
        
        stateMarker = (ClusteredStateMarker) new DummyClass();
        // Implementation note: tracker.track(int, Constructor, Object[]) is called during construction.
        modify().multiplicity(expect.from(0));
        
        tracker = stateMarker.$wadiGetTracker();
        modify().multiplicity(expect.from(0));
        
        instanceId = "instanceId";
        tracker.getInstanceId();
        modify().multiplicity(expect.from(0)).returnValue(instanceId);
        
        instanceRegistry = (InstanceRegistry) mock(InstanceRegistry.class);
        instanceRegistry.getInstance(instanceId);
        modify().multiplicity(expect.from(0)).returnValue(stateMarker);
        
        parentReplacer = new CompoundReplacer();
    }

    @ClusteredState
    protected static class DummyClass {
    }
    
}
