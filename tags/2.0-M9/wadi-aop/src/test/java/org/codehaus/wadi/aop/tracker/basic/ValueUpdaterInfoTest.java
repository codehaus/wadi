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
import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.core.reflect.ClassIndexer;
import org.codehaus.wadi.core.reflect.MemberUpdater;

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

    public void testNewValueUpdaterForConstructor() throws Exception {
        MemberUpdater memberUpdater = (MemberUpdater) mock(MemberUpdater.class);
        memberUpdater.getMember();
        modify().returnValue(DummyClass.class.getDeclaredConstructor());
        startVerification();
        
        ValueUpdaterInfo updaterInfo = new ValueUpdaterInfo(replacer, 1, new Object[0]);
        ValueUpdater valueUpdater = updaterInfo.newValueUpdater(memberUpdater);
        assertTrue(valueUpdater instanceof ConstructorInfo);
    }
    
    public void testNewValueUpdaterForMethod() throws Exception {
        MemberUpdater memberUpdater = (MemberUpdater) mock(MemberUpdater.class);
        memberUpdater.getMember();
        modify().returnValue(DummyClass.class.getDeclaredMethod("test"));
        startVerification();
        
        ValueUpdaterInfo updaterInfo = new ValueUpdaterInfo(replacer, 1, new Object[0]);
        ValueUpdater valueUpdater = updaterInfo.newValueUpdater(memberUpdater);
        assertTrue(valueUpdater instanceof MethodInfo);
    }
    
    public void testNewValueUpdaterForField() throws Exception {
        MemberUpdater memberUpdater = (MemberUpdater) mock(MemberUpdater.class);
        memberUpdater.getMember();
        modify().returnValue(DummyClass.class.getDeclaredField("name"));
        startVerification();
        
        ValueUpdaterInfo updaterInfo = new ValueUpdaterInfo(replacer, 1, new Object[0]);
        ValueUpdater valueUpdater = updaterInfo.newValueUpdater(memberUpdater);
        assertTrue(valueUpdater instanceof FieldInfo);
    }
    
    public void testInstanceTrackerIsReplacedByItsInstanceUponExecution() throws Exception {
        final ValueUpdater valueUpdater = (ValueUpdater) mock(ValueUpdater.class);
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
        classIndexer.getMemberUpdater(memberIndex);
        valueUpdater.executeWithParameters(instanceRegistry, instanceId, new Object[] {newStateMarker});
        
        startVerification();
        
        ValueUpdaterInfo updaterInfo = new ValueUpdaterInfo(replacer, memberIndex, new Object[] {stateMarker}) {
            @Override
            public ValueUpdaterInfo snapshotForSerialization() {
                return new ValueUpdaterInfo(this) {
                    @Override
                    protected ValueUpdater newValueUpdater(MemberUpdater memberUpdater) throws AssertionError {
                        return valueUpdater;
                    }
                };
            }
        };
        updaterInfo.setInstanceId(instanceId);
        updaterInfo = updaterInfo.snapshotForSerialization();
        updaterInfo.execute(instanceRegistry);
    }
    
    private static class DummyClass {
        private String name;
        private void test() {
        }
    }
    
}
