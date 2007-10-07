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

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.aop.tracker.InstanceTrackerVisitor;
import org.codehaus.wadi.aop.tracker.NoOpInstanceTrackerVisitor;
import org.codehaus.wadi.aop.tracker.VisitorContext;
import org.codehaus.wadi.aop.tracker.basic.CompoundReplacer;
import org.codehaus.wadi.aop.tracker.basic.ConstructorInfo;
import org.codehaus.wadi.aop.tracker.basic.InstanceAndTrackerReplacer;
import org.codehaus.wadi.aop.tracker.basic.ValueUpdaterInfo;
import org.codehaus.wadi.aop.tracker.visitor.CopyStateVisitor.CopyStateVisitorContext;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class CopyFullStateVisitorTest extends RMockTestCase {

    private InstanceTrackerVisitor setInstanceIdVisitor;
    private CopyFullStateVisitor visitor;
    private InstanceTracker instanceTracker;
    private InstanceAndTrackerReplacer replacer;

    @Override
    protected void setUp() throws Exception {
        replacer = new CompoundReplacer();
        
        setInstanceIdVisitor = (InstanceTrackerVisitor) mock(InstanceTrackerVisitor.class);
        
        visitor = new CopyFullStateVisitor(setInstanceIdVisitor);
        
        instanceTracker = (InstanceTracker) mock(InstanceTracker.class);
    }
    
    public void testSuccessfullCopy() throws Exception {
        VisitorContext setInstanceId = setInstanceIdVisitor.newContext();
        instanceTracker.visit(setInstanceIdVisitor, setInstanceId);
        
        instanceTracker.retrieveInstantiationValueUpdaterInfos();
        modify().returnValue(Collections.singletonList(
            new ValueUpdaterInfo(replacer, new ConstructorInfo(ArrayList.class.getDeclaredConstructor()), new Object[0])));
        
        instanceTracker.visit(NoOpInstanceTrackerVisitor.SINGLETON, null);
        modify().args(is.AS_RECORDED, is.NOT_NULL);
        
        startVerification();
        
        CopyStateVisitorContext context = visitor.newContext();
        visitor.visit(instanceTracker, context);
        
        byte[] serializedValueUpdaterInfos = context.getSerializedValueUpdaterInfos();
        
        ByteArrayInputStream memIn = new ByteArrayInputStream(serializedValueUpdaterInfos);
        ObjectInputStream in = new ObjectInputStream(memIn);
        List<ValueUpdaterInfo> actualValueUpdaterInfos = (List<ValueUpdaterInfo>) in.readObject();
        assertEquals(1, actualValueUpdaterInfos.size());
    }

}
