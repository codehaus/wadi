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
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.annotation.ClusteredState;
import org.codehaus.wadi.aop.aspectj.ClusteredStateAspectUtil;
import org.codehaus.wadi.aop.tracker.InstanceIdFactory;
import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.aop.tracker.visitor.CopyStateVisitor;
import org.codehaus.wadi.aop.tracker.visitor.ResetTrackingVisitor;
import org.codehaus.wadi.aop.tracker.visitor.SetInstanceIdVisitor;
import org.codehaus.wadi.aop.tracker.visitor.CopyStateVisitor.CopyStateVisitorContext;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicInstanceTrackerTest extends RMockTestCase {

    private InstanceIdFactory instanceIdFactory;

    @Override
    protected void setUp() throws Exception {
        instanceIdFactory = (InstanceIdFactory) mock(InstanceIdFactory.class);
        
        ClusteredStateAspectUtil.setInstanceTrackerFactory(new BasicInstanceTrackerFactory());
    }
    
    public void testApplyTo() throws Exception {
        instanceIdFactory.newId();
        modify().returnValue("a");
        
        instanceIdFactory.newId();
        modify().returnValue("b");
        
        startVerification();
        
        A a = new A();
        ClusteredStateMarker clusteredStateMarkerA = (ClusteredStateMarker) a;
        a.field = 123;
        
        B b = new B();
        a.b = b;
        b.field = 1234;
        b.a = a;
        
        CopyStateVisitor visitor = new CopyStateVisitor(new SetInstanceIdVisitor(instanceIdFactory),
            new ResetTrackingVisitor());
        CopyStateVisitorContext context = visitor.newContext();
        visitor.visit(clusteredStateMarkerA.$wadiGetTracker(), context);
        
        ByteArrayInputStream memIn = new ByteArrayInputStream(context.getSerializedInstanceTracker());
        ObjectInputStream in = new ObjectInputStream(memIn);
        InstanceTracker instanceTracker = (InstanceTracker) in.readObject();
        InstanceRegistry instanceRegistry = new BasicInstanceRegistry();
        instanceTracker.applyTo(instanceRegistry);
        
        A aCopy = (A) instanceRegistry.getInstance("a");
        assertEquals(a.field, aCopy.field);
        B bCopy = aCopy.b;
        assertNotNull(bCopy);
        assertEquals(b.field, bCopy.field);
        assertSame(aCopy, bCopy.a);
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
