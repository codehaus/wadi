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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

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
public class FieldInfoTest extends RMockTestCase {

    private InstanceTracker instanceTracker;
    private Field field;
    private FieldInfo fieldInfo;
    private InstanceRegistry instanceRegistry;

    @Override
    protected void setUp() throws Exception {
        instanceTracker = (InstanceTracker) mock(InstanceTracker.class);
        
        InstanceTrackerFactory trackerFactory = new InstanceTrackerFactory() {
            public InstanceTracker newInstanceTracker(ClusteredStateMarker stateMarker) {
                return instanceTracker;
            }    
        };
        ClusteredStateAspectUtil.setInstanceTrackerFactory(trackerFactory);
        
        instanceRegistry = new BasicInstanceRegistry();
        field = DummyClass.class.getDeclaredField("test");
        fieldInfo = new FieldInfo(field);
    }
    
    public void testSerialization() throws Exception {
        ByteArrayOutputStream memOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(memOut);
        out.writeObject(fieldInfo);
        out.close();
        
        ByteArrayInputStream memIn = new ByteArrayInputStream(memOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(memIn);
        FieldInfo serializedFieldInfo = (FieldInfo) in.readObject();
        assertEquals(serializedFieldInfo, fieldInfo);
    }
    
    public void testExecuteWithParameters() throws Exception {
        int testValue = 123;
        String instanceId = "instanceId";
        
        instanceTracker.track(0, (Constructor) null, null);
        modify().args(is.ANYTHING, is.ANYTHING, is.ANYTHING);
        
        instanceTracker.recordFieldUpdate(field, new Integer(testValue));
        startVerification();
        
        DummyClass instance = new DummyClass();
        instanceRegistry.registerInstance(instanceId, instance);
        
        fieldInfo.executeWithParameters(instanceRegistry, instanceId, new Object[] {new Integer(testValue)});
        assertEquals(testValue, instance.test);
    }
    
    @ClusteredState
    public static class DummyClass {
        private int test;
    }
    
}
