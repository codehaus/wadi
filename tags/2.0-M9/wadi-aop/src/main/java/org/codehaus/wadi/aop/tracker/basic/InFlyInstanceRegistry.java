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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceRegistryException;

/**
 * 
 * @version $Revision: 1538 $
 */
public class InFlyInstanceRegistry implements InstanceRegistry {
    private final InstanceRegistry instanceRegistry;
    private final Map<String, Object> instanceIdInstance;
    private final Set<String> unregisteredInstanceIds;
    
    public InFlyInstanceRegistry(InstanceRegistry instanceRegistry) {
        if (null == instanceRegistry) {
            throw new IllegalArgumentException("instanceRegistry is required");
        }
        this.instanceRegistry = instanceRegistry;
        
        instanceIdInstance = new HashMap<String, Object>();
        unregisteredInstanceIds = new HashSet<String>();
    }
    
    public Object getInstance(String instanceId) {
        Object instance = instanceIdInstance.get(instanceId);
        if (null == instance) {
            instance = instanceRegistry.getInstance(instanceId);
        }
        return instance;
    }

    public void registerInstance(String instanceId, Object instance) {
        if (instanceIdInstance.containsKey(instanceId)) {
            throw new InstanceRegistryException("Instance [" + instanceId + "] is already registered");
        }
        instanceIdInstance.put(instanceId, instance);
    }

    public void unregisterInstance(String instanceId) {
        unregisteredInstanceIds.add(instanceId);
    }
    
    public void merge() {
        for (Map.Entry<String, Object> entry : instanceIdInstance.entrySet()) {
            instanceRegistry.registerInstance(entry.getKey(), entry.getValue());
        }
        for (String instanceId : unregisteredInstanceIds) {
            instanceRegistry.unregisterInstance(instanceId);
        }
    }
    
}
