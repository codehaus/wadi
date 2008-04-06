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

import org.codehaus.wadi.aop.tracker.InstanceRegistryException;

import junit.framework.TestCase;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicInstanceRegistryTest extends TestCase {
    private BasicInstanceRegistry instanceRegistry;

    @Override
    protected void setUp() throws Exception {
        instanceRegistry = new BasicInstanceRegistry();
    }

    public void testWeakReference() throws Exception {
        for (int i = 0; i < 10000; i++) {
            instanceRegistry.registerInstance(i + "", new byte[1]);
        }

        int nbInstances = 0;
        for (int i = 0; i < 10000; i++) {
            try {
                instanceRegistry.getInstance(i + "");
                nbInstances++;
            } catch (InstanceRegistryException e) {
            }
        }
        assertTrue(nbInstances < 10000);
    }

    public void testInstanceRegistry() throws Exception {
        Object instance = new Object();
        String instanceId = "instanceId";
        instanceRegistry.registerInstance(instanceId, instance);
        assertSame(instance, instanceRegistry.getInstance(instanceId));
    }
    
    public void testDoubleRegistryOfSameInstanceIdThrowsException() throws Exception {
        String instanceId = "instanceId";
        instanceRegistry.registerInstance(instanceId, new Object());
        try {
            instanceRegistry.registerInstance(instanceId, new Object());
            fail();
        } catch (InstanceRegistryException e) {
        }
    }
    
    public void testGetUndefinedInstanceIdThrowsException() throws Exception {
        try {
            instanceRegistry.getInstance("instanceId");
            fail();
        } catch (InstanceRegistryException e) {
        }
    }
    
    public void testUnregistrationOfUndefinedIdThrowsException() throws Exception {
        try {
            instanceRegistry.unregisterInstance("instanceId");
            fail();
        } catch (InstanceRegistryException e) {
        }
    }
    
}
