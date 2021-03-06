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
package org.codehaus.wadi.aop.util;

import java.io.Serializable;

import org.codehaus.wadi.aop.annotation.ClusteredState;
import org.codehaus.wadi.aop.aspectj.ClusteredStateAspectUtil;
import org.codehaus.wadi.aop.reflect.ClusteredStateMemberFilter;
import org.codehaus.wadi.aop.tracker.InstanceIdFactory;
import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.basic.BasicInstanceRegistry;
import org.codehaus.wadi.aop.tracker.basic.BasicInstanceTrackerFactory;
import org.codehaus.wadi.aop.tracker.basic.BasicWireMarshaller;
import org.codehaus.wadi.aop.tracker.basic.CompoundReplacer;
import org.codehaus.wadi.aop.tracker.basic.InFlyInstanceRegistry;
import org.codehaus.wadi.aop.tracker.basic.InstanceAndTrackerReplacer;
import org.codehaus.wadi.aop.tracker.basic.WireMarshaller;
import org.codehaus.wadi.core.reflect.ClassIndexerRegistry;
import org.codehaus.wadi.core.reflect.jdk.JDKClassIndexerRegistry;
import org.codehaus.wadi.core.util.SimpleStreamer;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ClusteredStateHelperTest extends RMockTestCase {

    private WireMarshaller marshaller;
    private InstanceIdFactory instanceIdFactory;
    private InstanceRegistry instanceRegistry;

    @Override
    protected void setUp() throws Exception {
        InstanceAndTrackerReplacer replacer = new CompoundReplacer();
        ClassIndexerRegistry registry = new JDKClassIndexerRegistry(new ClusteredStateMemberFilter());

        SimpleStreamer streamer = new SimpleStreamer();
        marshaller = new BasicWireMarshaller(streamer, registry, replacer);

        instanceIdFactory = (InstanceIdFactory) mock(InstanceIdFactory.class);
        instanceRegistry = new InFlyInstanceRegistry(new BasicInstanceRegistry());
        
        ClusteredStateAspectUtil.resetInstanceTrackerFactory();
        ClusteredStateAspectUtil.setInstanceTrackerFactory(new BasicInstanceTrackerFactory(replacer, registry));
    }
    
    public void testFullSerialization() throws Exception {
        recordInstanceIdFactory();
        startVerification();
        
        A a = new A();
        a.field = 123;
        
        B b = new B();
        a.b = b;
        b.field = 1234;
        b.a = a;
        
        byte[] serialized = ClusteredStateHelper.serializeFully(instanceIdFactory, marshaller, a);
        ClusteredStateHelper.deserialize(instanceRegistry, marshaller, serialized);
        
        assertInstances(a, b, instanceRegistry);
    }

    public void testPartialSerialization() throws Exception {
        recordInstanceIdFactory();
        startVerification();
        
        A a = new A();
        a.field = 123;
        
        B b = new B();
        a.b = b;
        b.field = 1234;
        b.a = a;
        
        byte[] serialized = ClusteredStateHelper.serializeFully(instanceIdFactory, marshaller, a);
        ClusteredStateHelper.resetTracker(a);
        
        ClusteredStateHelper.deserialize(instanceRegistry, marshaller, serialized);

        a.field = 1234;
        b.field = 12345;
        
        serialized = ClusteredStateHelper.serialize(instanceIdFactory, marshaller, a);
        ClusteredStateHelper.deserialize(instanceRegistry, marshaller, serialized);
        
        assertInstances(a, b, instanceRegistry);
    }
    
    private void assertInstances(A a, B b, InstanceRegistry instanceRegistry) {
        A aCopy = (A) instanceRegistry.getInstance("a");
        assertEquals(a.field, aCopy.field);
        B bCopy = aCopy.b;
        assertNotNull(bCopy);
        assertEquals(b.field, bCopy.field);
        assertSame(aCopy, bCopy.a);
    }

    private void recordInstanceIdFactory() {
        instanceIdFactory.newId();
        modify().returnValue("a");
        
        instanceIdFactory.newId();
        modify().returnValue("b");
    }
    
    @ClusteredState
    private static class A implements Serializable {
        private int field;
        
        private B b;
    }
    
    @ClusteredState
    private static class B implements Serializable {
        private int field;
        
        private A a;
    }
    
}
