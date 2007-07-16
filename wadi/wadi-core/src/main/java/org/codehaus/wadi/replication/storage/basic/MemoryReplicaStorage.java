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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.storage.ReplicaKeyAlreadyExistsException;
import org.codehaus.wadi.replication.storage.ReplicaKeyNotFoundException;
import org.codehaus.wadi.replication.storage.ReplicaStorage;

/**
 * 
 * @version $Revision$
 */
public class MemoryReplicaStorage implements ReplicaStorage {
    private static final Log log = LogFactory.getLog(MemoryReplicaStorage.class);
    
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
            ReplicaInfo currentReplicaInfo = (ReplicaInfo) keyToReplica.get(key);
            if (null != currentReplicaInfo) {
                if (currentReplicaInfo.getVersion() > replicaInfo.getVersion()) {
                    throw new ReplicaKeyAlreadyExistsException(key);
                }
            }
            keyToReplica.put(key, replicaInfo);
        }
    }

    public void mergeUpdate(Object key, ReplicaInfo replicaInfo) {
        synchronized (keyToReplica) {
            ReplicaInfo currentReplicaInfo = (ReplicaInfo) keyToReplica.get(key);
            if (null == currentReplicaInfo) {
                throw new ReplicaKeyNotFoundException(key);
            }
            if (currentReplicaInfo.getVersion() > replicaInfo.getVersion()) {
                return;
            }
            currentReplicaInfo.mergeWith(replicaInfo);
        }
    }

    public void mergeDestroy(Object key) {
        synchronized (keyToReplica) {
            Object object = keyToReplica.remove(key);
            if (null == object) {
                log.warn("Key [" + key + "] is not defined; no replica to be removed.");
            }
        }
    }
    
    public ReplicaInfo retrieveReplicaInfo(Object key) {
        synchronized (keyToReplica) {
            return (ReplicaInfo) keyToReplica.get(key);
        }
    }

    public boolean storeReplicaInfo(Object key) {
        return null != retrieveReplicaInfo(key);
    }
    
}
