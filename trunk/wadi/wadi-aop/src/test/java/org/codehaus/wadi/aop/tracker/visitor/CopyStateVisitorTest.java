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
package org.codehaus.wadi.aop.tracker.visitor;

import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.aop.tracker.InstanceTrackerVisitor;
import org.codehaus.wadi.aop.tracker.basic.ValueUpdaterInfo;
import org.codehaus.wadi.aop.tracker.basic.WireMarshaller;
import org.codehaus.wadi.aop.tracker.visitor.CopyStateVisitor.CopyStateVisitorContext;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class CopyStateVisitorTest extends RMockTestCase {

    private WireMarshaller marshaller;
    private InstanceTrackerVisitor setInstanceIdVisitor;
    private InstanceTrackerVisitor resetTrackingVisitor;
    private CopyStateVisitor visitor;
    private InstanceTracker instanceTracker;

    @Override
    protected void setUp() throws Exception {
        marshaller = (WireMarshaller) mock(WireMarshaller.class);
        setInstanceIdVisitor = (InstanceTrackerVisitor) mock(InstanceTrackerVisitor.class);
        resetTrackingVisitor = (InstanceTrackerVisitor) mock(InstanceTrackerVisitor.class);
        
        visitor = new CopyStateVisitor(marshaller, setInstanceIdVisitor, resetTrackingVisitor);
        
        instanceTracker = (InstanceTracker) mock(InstanceTracker.class);
    }
    
    public void testSuccessfullCopy() throws Exception {
        instanceTracker.retrieveValueUpdaterInfos(setInstanceIdVisitor, resetTrackingVisitor);
        ValueUpdaterInfo[] valueUpdaterInfos = new ValueUpdaterInfo[0];
        modify().returnValue(valueUpdaterInfos);

        marshaller.marshall(valueUpdaterInfos);
        byte[] marshalled = new byte[0];
        modify().returnValue(marshalled);
        startVerification();
        
        CopyStateVisitorContext context = visitor.newContext();
        visitor.visit(instanceTracker, context);
        
        byte[] serializedValueUpdaterInfos = context.getSerializedValueUpdaterInfos();
        assertSame(marshalled, serializedValueUpdaterInfos);
    }
    
}
