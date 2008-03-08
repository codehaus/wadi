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

import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTracker;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class UnregisterTrackingVisitorTest extends RMockTestCase {

    public void testUnregsiter() throws Exception {
        InstanceTracker instanceTracker = (InstanceTracker) mock(InstanceTracker.class);
        InstanceRegistry instanceRegistry = (InstanceRegistry) mock(InstanceRegistry.class);

        instanceTracker.getInstanceId();
        String instanceId = "instanceId";
        modify().returnValue(instanceId);
        instanceRegistry.unregisterInstance(instanceId);
        startVerification();
        
        UnregisterTrackingVisitor visitor = new UnregisterTrackingVisitor(instanceRegistry);
        visitor.visit(instanceTracker, null);
    }

}
