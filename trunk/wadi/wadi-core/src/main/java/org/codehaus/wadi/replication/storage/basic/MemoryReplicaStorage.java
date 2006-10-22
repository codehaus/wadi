/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.replication.storage.basic;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.storage.ReplicaStorage;

/**
 * 
 * @version $Revision$
 */
public class MemoryReplicaStorage implements ReplicaStorage {
    private final Map keyToReplica;
    
    public MemoryReplicaStorage() {
        keyToReplica = new HashMap();
    }

    public void start() throws Exception {
    }

    public void stop() throws Exception {
        synchronized (keyToReplica) {
            keyToReplica.clear();
        }
    }
    
    public void mergeCreate(Object key, ReplicaInfo replicaInfo) {
        synchronized (keyToReplica) {
            if (keyToReplica.containsKey(key)) {
                throw new IllegalArgumentException("Key [" + key + "] is already defined");
            }
            keyToReplica.put(key, replicaInfo);
        }
    }

    public void mergeUpdate(Object key, ReplicaInfo replicaInfo) {
        synchronized (keyToReplica) {
            ReplicaInfo curReplicaInfo = (ReplicaInfo) keyToReplica.get(key);
            if (null == curReplicaInfo) {
                throw new IllegalArgumentException("Key [" + key + "] is not defined");
            }
            replicaInfo = new ReplicaInfo(curReplicaInfo, replicaInfo);
            keyToReplica.put(key, replicaInfo);
        }
    }

    public void mergeDestroy(Object key) {
        synchronized (keyToReplica) {
            Object object = keyToReplica.remove(key);
            if (null == object) {
                throw new IllegalArgumentException("Key [" + key + "] is not defined");
            }
        }
    }
    
    public ReplicaInfo retrieveReplicaInfo(Object key) {
        synchronized (keyToReplica) {
            return (ReplicaInfo) keyToReplica.get(key);
        }
    }

    public boolean storeReplicaInfo(Object key) {
        synchronized (keyToReplica) {
            return keyToReplica.containsKey(key);
        }
    }
    
}
