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

import java.lang.reflect.Field;

import org.codehaus.wadi.aop.annotation.ClusteredState;
import org.codehaus.wadi.aop.annotation.TrackingLevel;
import org.codehaus.wadi.aop.tracker.InstanceTracker;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class FieldTrackingLevelAspectTest extends RMockTestCase {

    private InstanceTracker instanceTracker;

    @Override
    protected void setUp() throws Exception {
        instanceTracker = (InstanceTracker) mock(InstanceTracker.class);
        
        AspectTestUtil.setUpInstanceTrackerFactory(instanceTracker);
    }
    
    public void testSetTrackedField() throws Exception {
        instanceTracker.track(0, FieldLevelTrackingClass.class.getConstructor(new Class[0]), null);
        modify().args(is.AS_RECORDED, is.AS_RECORDED, is.ANYTHING);

        Field testField = FieldLevelTrackingClass.class.getDeclaredField("test");
        Integer fieldValue = new Integer(1);
        instanceTracker.track(1, testField, fieldValue);
        instanceTracker.recordFieldUpdate(testField, fieldValue);
        
        startVerification();
        
        FieldLevelTrackingClass instance = new FieldLevelTrackingClass();
        instance.test = 1;
    }

    public void testSetTransientField() throws Exception {
        instanceTracker.track(0, FieldLevelTrackingClass.class.getConstructor(new Class[0]), null);
        modify().args(is.AS_RECORDED, is.AS_RECORDED, is.ANYTHING);
        startVerification();
        
        FieldLevelTrackingClass instance = new FieldLevelTrackingClass();
        instance.transientTest = 1;
    }
    
    public void testInheritanceOfTrackedField() throws Exception {
        instanceTracker.track(0, SubClass.class.getConstructor(new Class[0]), null);
        modify().args(is.AS_RECORDED, is.AS_RECORDED, is.ANYTHING);

        Field test2Field = SubClass.class.getDeclaredField("test2");
        Integer fieldValue = new Integer(1);
        instanceTracker.track(1, test2Field, fieldValue);
        instanceTracker.recordFieldUpdate(test2Field, fieldValue);
        
        Field testField = FieldLevelTrackingClass.class.getDeclaredField("test");
        instanceTracker.track(2, testField, fieldValue);
        instanceTracker.recordFieldUpdate(testField, fieldValue);

        startVerification();
        
        SubClass instance = new SubClass();
        instance.test2 = 1;
        instance.test = 1;

        instance.transientTest = 2;
        instance.transientTest2 = 2;
    }

    @ClusteredState(trackingLevel=TrackingLevel.FIELD)
    public static class FieldLevelTrackingClass {
        protected int test;
        
        protected transient int transientTest;
    }

    public static class SubClass extends FieldLevelTrackingClass {
        protected int test2;
        
        protected transient int transientTest2;
    }

}
