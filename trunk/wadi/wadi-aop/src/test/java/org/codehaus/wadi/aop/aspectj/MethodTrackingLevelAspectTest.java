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
package org.codehaus.wadi.aop.aspectj;

import java.io.IOException;
import java.lang.reflect.Method;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.annotation.ClusteredState;
import org.codehaus.wadi.aop.annotation.TrackedMethod;
import org.codehaus.wadi.aop.annotation.TrackingLevel;
import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.aop.tracker.InstanceTrackerFactory;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class MethodTrackingLevelAspectTest extends RMockTestCase {

    private InstanceTracker instanceTracker;

    @Override
    protected void setUp() throws Exception {
        instanceTracker = (InstanceTracker) mock(InstanceTracker.class);
        
        InstanceTrackerFactory trackerFactory = new InstanceTrackerFactory() {
            public InstanceTracker newInstanceTracker(ClusteredStateMarker stateMarker) {
                return instanceTracker;
            }    
        };
        ClusteredStateAspectUtil.resetInstanceTrackerFactory();
        ClusteredStateAspectUtil.setInstanceTrackerFactory(trackerFactory);
        
        instanceTracker.track(0, MethodLevelTrackingClass.class.getConstructor(new Class[0]), null);
        modify().args(is.ANYTHING, is.AS_RECORDED, is.ANYTHING);
        instanceTracker.recordFieldUpdate(MethodLevelTrackingClass.class.getDeclaredField("childMethod"), null);
        modify().args(is.AS_RECORDED, is.ANYTHING);
        instanceTracker.recordFieldUpdate(MethodLevelTrackingClass.class.getDeclaredField("childField"), null);
        modify().args(is.AS_RECORDED, is.ANYTHING);
        
        instanceTracker.track(0, ChildMethodLevelTrackingClass.class.getConstructor(new Class[0]), null);
        modify().args(is.ANYTHING, is.AS_RECORDED, is.ANYTHING);
        
        instanceTracker.track(0, ChildFieldLevelTrackingClass.class.getConstructor(new Class[0]), null);
        modify().args(is.ANYTHING, is.AS_RECORDED, is.ANYTHING);
    }
    
    public void testExecuteTrackedMethod() throws Exception {
        instanceTracker.track(1,
            MethodLevelTrackingClass.class.getDeclaredMethod("test", new Class[] { int.class }),
            null);
        final Integer fieldValue = new Integer(123);
        modify().args(is.ANYTHING, is.AS_RECORDED, new AbstractExpression() {

            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                Object[] args = (Object[]) arg0;
                assertEquals(fieldValue, args[0]);
                return true;
            }
            
        });
        instanceTracker.recordFieldUpdate(MethodLevelTrackingClass.class.getDeclaredField("test"), fieldValue);
        
        startVerification();
        
        MethodLevelTrackingClass instance = new MethodLevelTrackingClass();
        int result = instance.test(fieldValue);
        assertEquals(246, result);
    }
    
    public void testExecuteStandardMethod() throws Exception {
        Integer fieldValue = new Integer(123);
        instanceTracker.recordFieldUpdate(MethodLevelTrackingClass.class.getDeclaredField("test"), fieldValue);
        startVerification();
        
        MethodLevelTrackingClass instance = new MethodLevelTrackingClass();
        instance.transientTest(fieldValue);
    }
    
    public void testMethodInvokeMethodAndOnlyRootMethodIsActuallyTracked() throws Exception {
        instanceTracker.track(1, (Method) null, null);
        modify().args(is.ANYTHING, is.ANYTHING, is.ANYTHING);
        
        Integer fieldValue = new Integer(123);
        instanceTracker.recordFieldUpdate(ChildMethodLevelTrackingClass.class.getDeclaredField("test"), fieldValue);

        startVerification();
        
        MethodLevelTrackingClass instance = new MethodLevelTrackingClass();
        instance.invokeChildMethod(fieldValue);
    }
    
    public void testMethodSetFieldAndOnlyRootMethodIsActuallyTracked() throws Exception {
        instanceTracker.track(1, (Method) null, null);
        modify().args(is.ANYTHING ,is.ANYTHING, is.ANYTHING);
        
        Integer fieldValue = 123;
        instanceTracker.recordFieldUpdate(ChildFieldLevelTrackingClass.class.getDeclaredField("test"), fieldValue);
        
        startVerification();
        
        MethodLevelTrackingClass instance = new MethodLevelTrackingClass();
        instance.invokeChildField(fieldValue);
    }
    
    public void testMethodThrowExceptionIsNotTracked() throws Exception {
        startVerification();
        
        MethodLevelTrackingClass instance = new MethodLevelTrackingClass();
        try {
            instance.testThrowException();
            fail();
        } catch (UnsupportedOperationException e) {
        }
    }
    
    @ClusteredState(trackingLevel=TrackingLevel.METHOD)
    public static class MethodLevelTrackingClass {
        private ChildMethodLevelTrackingClass childMethod = new ChildMethodLevelTrackingClass();
        private ChildFieldLevelTrackingClass childField = new ChildFieldLevelTrackingClass();
        private int test;
        
        @TrackedMethod
        protected int test(int test) {
            this.test = test;
            return test * 2;
        }
        
        @TrackedMethod
        protected void testThrowException() {
            throw new UnsupportedOperationException();
        }
        
        protected void transientTest(int test) {
            this.test = test;
        }
        
        @TrackedMethod
        protected void invokeChildMethod(int test) {
            childMethod.test(test);
        }
        
        @TrackedMethod
        protected void invokeChildField(int test) {
            childField.test = test;
        }
    }
    
    @ClusteredState(trackingLevel=TrackingLevel.METHOD)
    public static class ChildMethodLevelTrackingClass {
        private int test;
        
        @TrackedMethod
        private void test(int test) {
            this.test = test;
        }
    }
    
    @ClusteredState(trackingLevel=TrackingLevel.FIELD)
    public static class ChildFieldLevelTrackingClass {
        private int test;
    }
    
}
