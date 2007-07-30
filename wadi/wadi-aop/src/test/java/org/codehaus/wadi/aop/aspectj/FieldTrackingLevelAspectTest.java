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

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.annotation.ClusteredState;
import org.codehaus.wadi.aop.annotation.TrackingLevel;
import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.aop.tracker.InstanceTrackerFactory;

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
        
        InstanceTrackerFactory trackerFactory = new InstanceTrackerFactory() {
            public InstanceTracker newInstanceTracker(ClusteredStateMarker stateMarker) {
                return instanceTracker;
            }    
        };
        ClusteredStateAspectUtil.setInstanceTrackerFactory(trackerFactory);
        
        instanceTracker.track(0, FieldLevelTrackingClass.class.getConstructor(new Class[0]), null);
        modify().args(is.ANYTHING, is.AS_RECORDED, is.ANYTHING);
    }
    
    public void testSetTrackedField() throws Exception {
        Field testField = FieldLevelTrackingClass.class.getDeclaredField("test");
        Integer fieldValue = new Integer(1);
        instanceTracker.track(1, testField, fieldValue);
        instanceTracker.recordFieldUpdate(testField, fieldValue);
        
        startVerification();
        
        FieldLevelTrackingClass instance = new FieldLevelTrackingClass();
        instance.test = 1;
    }

    public void testSetTransientField() throws Exception {
        startVerification();
        
        FieldLevelTrackingClass instance = new FieldLevelTrackingClass();
        instance.transientTest = 1;
    }
    
    @ClusteredState(trackingLevel=TrackingLevel.FIELD)
    public static class FieldLevelTrackingClass {
        protected int test;
        
        protected transient int transientTest;
    }
    
}
