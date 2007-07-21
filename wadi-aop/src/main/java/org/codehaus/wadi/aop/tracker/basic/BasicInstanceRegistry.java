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
import java.util.Map;

import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceRegistryException;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicInstanceRegistry implements InstanceRegistry {
    private final Map<String, Object> instanceIdToInstance;
    
    public BasicInstanceRegistry() {
        instanceIdToInstance = new HashMap<String, Object>();
    }
    
    public Object getInstance(String instanceId) {
        Object instance = instanceIdToInstance.get(instanceId);
        if (null == instance) {
            throw new InstanceRegistryException("Instance [" + instanceId + "] does not exist");
        }
        return instance;
    }

    public void registerInstance(String instanceId, Object instance) {
        if (instanceIdToInstance.containsKey(instanceId)) {
            throw new InstanceRegistryException("Instance [" + instanceId + "] is already registered");
        }
        instanceIdToInstance.put(instanceId, instance);
    }

    public void unregisterInstance(String instanceId) {
        Object instance = instanceIdToInstance.remove(instanceId);
        if (null == instance) {
            throw new InstanceRegistryException("Instance [" + instanceId + "] does not exist");
        }
    }
}
