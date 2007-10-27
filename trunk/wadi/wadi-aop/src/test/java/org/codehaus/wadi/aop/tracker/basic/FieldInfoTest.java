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

import java.lang.reflect.Field;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.core.reflect.MemberUpdater;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class FieldInfoTest extends RMockTestCase {

    private InstanceTracker instanceTracker;
    private Field field;
    private FieldInfo fieldInfo;
    private InstanceRegistry instanceRegistry;
    private MemberUpdater memberUpdater;

    @Override
    protected void setUp() throws Exception {
        memberUpdater = (MemberUpdater) mock(MemberUpdater.class);
        instanceTracker = (InstanceTracker) mock(InstanceTracker.class);
        
        instanceRegistry = (InstanceRegistry) mock(InstanceRegistry.class);
        field = DummyClass.class.getDeclaredField("test");
        memberUpdater.getMember();
        modify().multiplicity(expect.from(0)).returnValue(field);
        fieldInfo = new FieldInfo(memberUpdater);
    }
    
    public void testExecuteWithParameters() throws Exception {
        int testValue = 123;
        String instanceId = "instanceId";
        Object[] parameters = new Object[] {new Integer(testValue)};

        ClusteredStateMarker instance = (ClusteredStateMarker) mock(ClusteredStateMarker.class);

        instanceRegistry.getInstance(instanceId);
        modify().returnValue(instance);
        
        memberUpdater.executeWithParameters(instance, parameters);
        
        instance.$wadiGetTracker();
        modify().returnValue(instanceTracker);
        instanceTracker.recordFieldUpdate(field, parameters[0]);
        startVerification();
        
        fieldInfo.executeWithParameters(instanceRegistry, instanceId, parameters);
    }
    
    public static class DummyClass {
        private int test;
    }
    
}
