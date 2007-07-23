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
import java.util.Map;

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
        instanceTracker.track(1, FieldLevelTrackingClass.class.getDeclaredField("test"), new Integer(1));
        
        startVerification();
        
        FieldLevelTrackingClass instance = new FieldLevelTrackingClass();
        instance.test = 1;
    }

    public void testGetFieldValues() throws Exception {
        Field field = FieldLevelTrackingClass.class.getDeclaredField("test");
        instanceTracker.track(1, (Field) null, null);
        modify().multiplicity(expect.from(1)).args(is.ANYTHING, is.NOT_NULL, is.NOT_NULL);
        
        startVerification();
        
        FieldLevelTrackingClass instance = new FieldLevelTrackingClass();
        instance.test = 1;
        instance.test = 2;
        instance.test = 3;
        
        ClusteredStateMarker clusteredStateMarker = (ClusteredStateMarker) instance;
        Map fieldValues = clusteredStateMarker.$wadiGetFieldValues();
        assertEquals(1, fieldValues.size());
        Integer value = (Integer) fieldValues.get(field);
        assertEquals(new Integer(3), value);
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
