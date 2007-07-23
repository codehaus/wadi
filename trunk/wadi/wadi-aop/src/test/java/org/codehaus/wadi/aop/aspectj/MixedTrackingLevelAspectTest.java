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

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.annotation.ClusteredState;
import org.codehaus.wadi.aop.annotation.TrackedMethod;
import org.codehaus.wadi.aop.annotation.TrackingLevel;
import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.aop.tracker.InstanceTrackerFactory;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class MixedTrackingLevelAspectTest extends RMockTestCase {

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
        
        instanceTracker.track(0, MixedLevelTrackingClass.class.getConstructor(new Class[0]), null);
        modify().args(is.ANYTHING, is.AS_RECORDED, is.ANYTHING);
    }
    
    public void testSetTrackedField() throws Exception {
        instanceTracker.track(1, MixedLevelTrackingClass.class.getDeclaredField("test"), new Integer(1));
        modify().args(is.ANYTHING, is.AS_RECORDED, is.AS_RECORDED);
        
        startVerification();
        
        MixedLevelTrackingClass instance = new MixedLevelTrackingClass();
        instance.test = 1;
    }
    
    public void testSetTransientField() throws Exception {
        startVerification();
        
        MixedLevelTrackingClass instance = new MixedLevelTrackingClass();
        instance.transientTest = 1;
    }

    public void testInvokeTrackedMethodWhichSetTrackedFieldOnlyTrackMethod() throws Exception {
        instanceTracker.track(1,
            MixedLevelTrackingClass.class.getDeclaredMethod("setTest", new Class[] { int.class }),
            null);
        modify().args(is.ANYTHING, is.AS_RECORDED, is.ANYTHING);
        
        startVerification();
        
        MixedLevelTrackingClass instance = new MixedLevelTrackingClass();
        instance.setTest(1);
    }

    public void testInvokeTransientMethod() throws Exception {
        startVerification();
        
        MixedLevelTrackingClass instance = new MixedLevelTrackingClass();
        instance.setTestTransientMethod(1);
    }
    
    @ClusteredState(trackingLevel=TrackingLevel.MIXED)
    public static class MixedLevelTrackingClass {
        private int test;
        
        private transient int transientTest;
        
        @TrackedMethod
        public void setTest(int test) throws Exception {
            this.test = test;
        }
        
        public void setTestTransientMethod(int transientTest) throws Exception {
            this.transientTest = transientTest;
        }
    }
    
}
