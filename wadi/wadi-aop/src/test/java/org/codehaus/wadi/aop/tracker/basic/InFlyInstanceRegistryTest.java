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

import org.codehaus.wadi.aop.tracker.InstanceRegistry;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class InFlyInstanceRegistryTest extends RMockTestCase {

    private InstanceRegistry instanceRegistry;
    private InFlyInstanceRegistry inFlyInstanceRegistry;
    private String instanceId;
    private Object instance;

    @Override
    protected void setUp() throws Exception {
        instanceRegistry = (InstanceRegistry) mock(InstanceRegistry.class);
        instanceId = "instanceId";
        instance = new Object();
        
        inFlyInstanceRegistry = new InFlyInstanceRegistry(instanceRegistry);
    }

    public void testGetInFlyInstance() throws Exception {
        inFlyInstanceRegistry.registerInstance(instanceId, instance);
        
        assertSame(instance, inFlyInstanceRegistry.getInstance(instanceId));
    }
    
    public void testGetNotInFlyInstance() throws Exception {
        instanceRegistry.getInstance(instanceId);
        modify().returnValue(instance);
        
        startVerification();
        
        assertSame(instance, inFlyInstanceRegistry.getInstance(instanceId));
    }

    public void testUnregistrationIsMerged() throws Exception {
        instanceRegistry.unregisterInstance(instanceId);
        
        startVerification();
        
        inFlyInstanceRegistry.unregisterInstance(instanceId);
        inFlyInstanceRegistry.merge();
    }
    
    public void testRegistrationIsMerged() throws Exception {
        instanceRegistry.registerInstance(instanceId, instance);
        
        startVerification();
        
        inFlyInstanceRegistry.registerInstance(instanceId, instance);
        inFlyInstanceRegistry.merge();
    }
    
}
