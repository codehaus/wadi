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

import java.util.Set;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.reflect.ClassIndexer;
import org.codehaus.wadi.aop.reflect.MemberUpdater;
import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTracker;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ValueUpdaterInfoTest extends RMockTestCase {

    private InstanceAndTrackerReplacer replacer;

    @Override
    protected void setUp() throws Exception {
        replacer = new CompoundReplacer();
    }
    
    public void testInstanceIdCanBeSetOnce() throws Exception {
        ValueUpdaterInfo updaterInfo = new ValueUpdaterInfo(replacer, 1, new Object[0]);
        updaterInfo.setInstanceId("instanceId");
        try {
            updaterInfo.setInstanceId("instanceId");
            fail();
        } catch (IllegalStateException e) {
        }
    }
    
    public void testClusteredStateMarkersAreTransformedIntoInstanceTrackers() throws Exception {
        ClusteredStateMarker stateMarker = (ClusteredStateMarker) mock(ClusteredStateMarker.class);
        InstanceTracker instanceTracker = stateMarker.$wadiGetTracker();
        
        startVerification();
        
        String param0 = "test";
        ValueUpdaterInfo updaterInfo = new ValueUpdaterInfo(replacer, 1, new Object[] {param0, stateMarker});
        Set<InstanceTracker> instanceTrackers = updaterInfo.getInstanceTrackers();
        assertEquals(1, instanceTrackers.size());
        assertTrue(instanceTrackers.contains(instanceTracker));

        Object[] newParameters = updaterInfo.getParametersReplacedWithTrackers();
        assertSame(param0, newParameters[0]);
        assertSame(instanceTracker, newParameters[1]);
    }

    public void testInstanceTrackerIsReplacedByItsInstanceUponExecution() throws Exception {
        String instanceId = "instanceId";
        int memberIndex = 1;

        ClusteredStateMarker stateMarker = (ClusteredStateMarker) mock(ClusteredStateMarker.class);
        InstanceTracker instanceTracker = stateMarker.$wadiGetTracker();
        instanceTracker.getInstanceId();
        modify().returnValue(instanceId);
        
        ClusteredStateMarker newStateMarker = (ClusteredStateMarker) mock(ClusteredStateMarker.class);
        InstanceRegistry instanceRegistry = (InstanceRegistry) mock(InstanceRegistry.class);
        instanceRegistry.getInstance(instanceId);
        modify().multiplicity(expect.from(0)).returnValue(newStateMarker);
        InstanceTracker newInstanceTracker = newStateMarker.$wadiGetTracker();
        ClassIndexer classIndexer = newInstanceTracker.getClassIndexer();
        
        MemberUpdater memberUpdater = classIndexer.getMemberUpdater(memberIndex);
        ValueUpdater valueUpdater = memberUpdater.getValueUpdater();
        valueUpdater.executeWithParameters(instanceRegistry, instanceId, new Object[] {newStateMarker});
        
        startVerification();
        
        ValueUpdaterInfo updaterInfo = new ValueUpdaterInfo(replacer, memberIndex, new Object[] {stateMarker});
        updaterInfo.setInstanceId(instanceId);
        updaterInfo = updaterInfo.snapshotForSerialization();
        updaterInfo.execute(instanceRegistry);
    }
    
}
