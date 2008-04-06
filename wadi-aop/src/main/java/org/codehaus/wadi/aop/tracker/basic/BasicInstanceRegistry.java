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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.aop.tracker.InstanceRegistry;
import org.codehaus.wadi.aop.tracker.InstanceRegistryException;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicInstanceRegistry implements InstanceRegistry {
    private final Map<String, WeakReferenceWithMapKey<Object>> instanceIdToReference;
    private final ReferenceQueue<Object> refQueue;
    
    public BasicInstanceRegistry() {
        instanceIdToReference = new HashMap<String, WeakReferenceWithMapKey<Object>>();
        refQueue = new ReferenceQueue<Object>();
    }
    
    public Object getInstance(String instanceId) {
        sweepMap();

        WeakReference<Object> softReference;
        synchronized (instanceIdToReference) {
            softReference = instanceIdToReference.get(instanceId);
        }
        if (null == softReference) {
            throw new InstanceRegistryException("Instance [" + instanceId + "] does not exist");
        }
        return softReference.get();
    }

    public void registerInstance(String instanceId, Object instance) {
        sweepMap();
        
        synchronized (instanceIdToReference) {
            if (instanceIdToReference.containsKey(instanceId)) {
                throw new InstanceRegistryException("Instance [" + instanceId + "] is already registered");
            }
            instanceIdToReference.put(instanceId, new WeakReferenceWithMapKey<Object>(instanceId, instance, refQueue));
        }
    }

    public void unregisterInstance(String instanceId) {
        sweepMap();

        WeakReference<Object> softReference;
        synchronized (instanceIdToReference) {
            softReference = instanceIdToReference.remove(instanceId);
        }
        if (null == softReference) {
            throw new InstanceRegistryException("Instance [" + instanceId + "] does not exist");
        }
    }
    
    protected void sweepMap() {
        Reference ref;
        synchronized (refQueue) {
            while (null != (ref = refQueue.poll())) {
                instanceIdToReference.remove(((WeakReferenceWithMapKey) ref).getMapKey());   
            }
        }
    }
    
    protected static class WeakReferenceWithMapKey<T> extends WeakReference<T> {

        private final String mapKey;

        public WeakReferenceWithMapKey(String mapKey, T referent, ReferenceQueue<? super T> q) {
            super(referent, q);
            if (null == mapKey) {
                throw new IllegalArgumentException("mapKey is required");
            }
            this.mapKey = mapKey;
        }

        public String getMapKey() {
            return mapKey;
        }
        
    }
    
}
