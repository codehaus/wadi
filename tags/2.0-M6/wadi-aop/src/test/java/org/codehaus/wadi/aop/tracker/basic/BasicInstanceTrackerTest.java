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
import java.util.List;

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.tracker.InstanceTrackerVisitor;
import org.codehaus.wadi.aop.tracker.VisitorContext;
import org.codehaus.wadi.aop.tracker.visitor.BaseVisitorContext;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicInstanceTrackerTest extends RMockTestCase {

    private Field trackerField;
    private ClusteredStateMarker stateMarkerParent;
    private BasicInstanceTracker instanceTrackerParent;
    private ClusteredStateMarker stateMarkerChild;
    private BasicInstanceTracker instanceTrackerChild;

    @Override
    protected void setUp() throws Exception {
        trackerField = BasicBean.class.getDeclaredField("tracker");
        
        stateMarkerParent = (ClusteredStateMarker) mock(ClusteredStateMarker.class);
        instanceTrackerParent = new BasicInstanceTracker(stateMarkerParent);
        instanceTrackerParent.setInstanceId("instanceTrackerParent");
        stateMarkerParent.$wadiGetTracker();
        modify().multiplicity(expect.from(0)).returnValue(instanceTrackerParent);
        
        stateMarkerChild = (ClusteredStateMarker) mock(ClusteredStateMarker.class);
        instanceTrackerChild = new BasicInstanceTracker(stateMarkerChild);
        instanceTrackerChild.setInstanceId("instanceTrackerChild");
        stateMarkerChild.$wadiGetTracker();
        modify().multiplicity(expect.from(0)).returnValue(instanceTrackerChild);
    }
    
    public void testVisitCircularInstanceTrackers() throws Exception {
        InstanceTrackerVisitor visitor = (InstanceTrackerVisitor) mock(InstanceTrackerVisitor.class);
        VisitorContext visitorContext = new BaseVisitorContext();
        
        beginSection(s.ordered("Visit trackers"));
        visitor.visit(instanceTrackerParent, visitorContext);
        visitor.visit(instanceTrackerChild, visitorContext);
        endSection();
        startVerification();
        
        instanceTrackerParent.recordFieldUpdate(trackerField, stateMarkerChild);
        instanceTrackerChild.recordFieldUpdate(trackerField, stateMarkerParent);
        instanceTrackerParent.visit(visitor, visitorContext);
    }
    
    public void testRetrieveValueUpdaterInfos() throws Exception {
        startVerification();
        
        instanceTrackerParent.track(1, trackerField, stateMarkerChild);
        instanceTrackerParent.recordFieldUpdate(trackerField, stateMarkerChild);

        instanceTrackerChild.track(2, trackerField, stateMarkerParent);
        instanceTrackerChild.recordFieldUpdate(trackerField, stateMarkerParent);
        
        List<ValueUpdaterInfo> valueUpdaterInfos = instanceTrackerParent.retrieveValueUpdaterInfos();
        assertEquals(2, valueUpdaterInfos.size());
    }
    
    private static class BasicBean {
        private Object tracker;
    }
    
}
