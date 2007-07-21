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
package org.codehaus.wadi.replication.storage.memory;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.replication.common.ReplicaStorageInfo;
import org.codehaus.wadi.replication.manager.basic.ObjectStateHandler;
import org.codehaus.wadi.replication.storage.ReplicaKeyAlreadyExistsException;
import org.codehaus.wadi.replication.storage.ReplicaKeyNotFoundException;
import org.codehaus.wadi.replication.storage.ReplicaStorage;

/**
 * 
 * @version $Revision$
 */
public class MemoryReplicaStorage implements ReplicaStorage {
    private static final Log log = LogFactory.getLog(MemoryReplicaStorage.class);
    
    private final Map<Object, ReplicaStorageInfo> keyToStorageInfo;
    private final ObjectStateHandler objectStateManager;
    
    public MemoryReplicaStorage(ObjectStateHandler objectStateManager) {
        if (null == objectStateManager) {
            throw new IllegalArgumentException("objectStateManager is required");
        }
        this.objectStateManager = objectStateManager;
        
        keyToStorageInfo = new HashMap<Object, ReplicaStorageInfo>();
    }

    public void start() throws Exception {
    }

    public void stop() throws Exception {
        synchronized (keyToStorageInfo) {
            keyToStorageInfo.clear();
        }
    }
    
    public void mergeCreate(Object key, ReplicaStorageInfo createStorageInfo) throws ReplicaKeyAlreadyExistsException {
        ReplicaStorageInfo storageInfo;
        synchronized (keyToStorageInfo) {
            storageInfo = keyToStorageInfo.get(key);
            if (null == storageInfo) {
                storageInfo = createStorageInfo;
            } else if (storageInfo.getVersion() > createStorageInfo.getVersion()) {
                throw new ReplicaKeyAlreadyExistsException(key);
            }
            keyToStorageInfo.put(key, storageInfo);
        }
        
        synchronized (storageInfo) {
            Object payload = objectStateManager.restoreFromFullState(key, storageInfo.getSerializedPayload());
            storageInfo.getReplicaInfo().setPayload(payload);
        }
    }

    public void mergeUpdate(Object key, ReplicaStorageInfo updateStorageInfo) throws ReplicaKeyNotFoundException {
        ReplicaStorageInfo storageInfo;
        synchronized (keyToStorageInfo) {
            storageInfo = keyToStorageInfo.get(key);
            if (null == storageInfo) {
                throw new ReplicaKeyNotFoundException(key);
            } else if (storageInfo.getVersion() > updateStorageInfo.getVersion()) {
                throw new ReplicaKeyAlreadyExistsException(key);
            }
            storageInfo = updateStorageInfo;
            keyToStorageInfo.put(key, storageInfo);
        }

        synchronized (storageInfo) {
            Object payload = objectStateManager.restoreFromUpdatedState(key, storageInfo.getSerializedPayload());
            storageInfo.getReplicaInfo().setPayload(payload);
        }
    }

    public void mergeDestroy(Object key) {
        synchronized (keyToStorageInfo) {
            ReplicaStorageInfo remove = keyToStorageInfo.remove(key);
            if (null == remove) {
                log.warn("Key [" + key + "] is not defined; no replica to be removed.");
            }
        }
    }
    
    public ReplicaStorageInfo retrieveReplicaStorageInfo(Object key) {
        synchronized (keyToStorageInfo) {
            return keyToStorageInfo.get(key);
        }
    }
    
    public boolean storeReplicaInfo(Object key) {
        return null != retrieveReplicaStorageInfo(key);
    }
    
}
