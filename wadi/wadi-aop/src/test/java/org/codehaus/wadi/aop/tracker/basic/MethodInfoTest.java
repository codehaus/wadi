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

import org.codehaus.wadi.aop.reflect.MemberUpdater;
import org.codehaus.wadi.aop.tracker.InstanceRegistry;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class MethodInfoTest extends RMockTestCase {

    private MethodInfo methodInfo;
    private InstanceRegistry instanceRegistry;
    private MemberUpdater memberUpdater;

    @Override
    protected void setUp() throws Exception {
        memberUpdater = (MemberUpdater) mock(MemberUpdater.class);
        instanceRegistry = (InstanceRegistry) mock(InstanceRegistry.class);
        methodInfo = new MethodInfo(memberUpdater);
    }
    
    public void testExecuteWithParameters() throws Exception {
        int testValue = 123;
        String instanceId = "instanceId";
        Object instance = new Object();
        Object[] parameters = new Object[] {new Integer(testValue)};
        
        instanceRegistry.getInstance(instanceId);
        modify().returnValue(instance);

        memberUpdater.executeWithParameters(instance, parameters);
        
        startVerification();
        
        methodInfo.executeWithParameters(instanceRegistry, instanceId, parameters);
    }
    
}
