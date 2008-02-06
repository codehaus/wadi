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

import org.codehaus.wadi.aop.ClusteredStateMarker;
import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceTracker;
import org.codehaus.wadi.core.reflect.MemberUpdater;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ConstructorInfoTest extends RMockTestCase {

    private InstanceTracker instanceTracker;
    private ConstructorInfo constructorInfo;
    private InstanceRegistry instanceRegistry;
    private MemberUpdater memberUpdater;

    @Override
    protected void setUp() throws Exception {
        memberUpdater = (MemberUpdater) mock(MemberUpdater.class);
        instanceTracker = (InstanceTracker) mock(InstanceTracker.class);
        instanceRegistry = (InstanceRegistry) mock(InstanceRegistry.class);
        constructorInfo = new ConstructorInfo(memberUpdater);
    }
    
    public void testExecuteWithParameters() throws Exception {
        final int testValue = 123;
        String instanceId = "instanceId";
        Object[] parameters = new Object[] {new Integer(testValue)};
        ClusteredStateMarker stateMarker = (ClusteredStateMarker) mock(ClusteredStateMarker.class);
        
        memberUpdater.executeWithParameters(null, parameters);
        modify().returnValue(stateMarker);
        
        stateMarker.$wadiGetTracker();
        modify().returnValue(instanceTracker);
        
        instanceTracker.setInstanceId(instanceId);
        
        instanceRegistry.registerInstance(instanceId, stateMarker);
        startVerification();
        
        constructorInfo.executeWithParameters(instanceRegistry, instanceId, parameters);
    }
    
}
