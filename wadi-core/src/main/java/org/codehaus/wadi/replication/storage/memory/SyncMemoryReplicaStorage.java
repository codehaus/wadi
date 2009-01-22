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
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.common.ReplicaStorageInfo;
import org.codehaus.wadi.replication.manager.basic.ObjectStateHandler;
import org.codehaus.wadi.replication.storage.ReplicaKeyAlreadyExistsException;
import org.codehaus.wadi.replication.storage.ReplicaKeyNotFoundException;
import org.codehaus.wadi.replication.storage.ReplicaStorage;

/**
 * 
 * @version $Revision$
 */
public class SyncMemoryReplicaStorage implements ReplicaStorage {
    private static final Log log = LogFactory.getLog(SyncMemoryReplicaStorage.class);
    
    private final Map<Object, ReplicaStorageInfo> keyToStorageInfo;
    private final ObjectStateHandler objectStateHandler;
    
    public SyncMemoryReplicaStorage(ObjectStateHandler objectStateHandler) {
        if (null == objectStateHandler) {
            throw new IllegalArgumentException("objectStateHandler is required");
        }
        this.objectStateHandler = objectStateHandler;
        
        keyToStorageInfo = new HashMap<Object, ReplicaStorageInfo>();
    }

    public void start() throws Exception {
    }

    public void stop() throws Exception {
        synchronized (keyToStorageInfo) {
            keyToStorageInfo.clear();
        }
    }
    
    public void insert(Object key, ReplicaInfo replicaInfo) throws ReplicaKeyAlreadyExistsException {
        synchronized (keyToStorageInfo) {
            if (keyToStorageInfo.containsKey(key)) {
                throw new ReplicaKeyAlreadyExistsException(key);
                
            }
            keyToStorageInfo.put(key, new ReplicaStorageInfo(replicaInfo, new byte[0]));
        }
        
        objectStateHandler.initState(key, replicaInfo.getPayload());
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
        
        Motable payload = objectStateHandler.restoreFromFullState(key, storageInfo.getSerializedPayload());
        storageInfo.getReplicaInfo().setPayload(payload);
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
            // Implementation note: keep a strong reference to the payload otherwise, it can be GCed and, hence, we
            // can not apply a delta to it.
            Motable payload = storageInfo.getReplicaInfo().getPayload();
            updateStorageInfo.getReplicaInfo().setPayload(payload);
            storageInfo = updateStorageInfo;
            keyToStorageInfo.put(key, storageInfo);
        }

        Motable payload = objectStateHandler.restoreFromUpdatedState(key, storageInfo.getSerializedPayload());
        storageInfo.getReplicaInfo().setPayload(payload);
    }

    public void mergeDestroy(Object key) {
        ReplicaStorageInfo remove;
        synchronized (keyToStorageInfo) {
            remove = keyToStorageInfo.remove(key);
        }
        if (null == remove) {
            log.warn("Key [" + key + "] is not defined; no replica to be removed.");
            return;
        }
        objectStateHandler.discardState(key, remove.getReplicaInfo().getPayload());
    }
    
    public void mergeDestroyIfExist(Object key) {
        ReplicaStorageInfo remove;
        synchronized (keyToStorageInfo) {
            remove = keyToStorageInfo.remove(key);
        }
        if (null == remove) {
            return;
        }
        objectStateHandler.discardState(key, remove.getReplicaInfo().getPayload());
    }
    
    public ReplicaStorageInfo retrieveReplicaStorageInfo(Object key) {
        ReplicaStorageInfo storageInfo;
        synchronized (keyToStorageInfo) {
            storageInfo = keyToStorageInfo.get(key);
        }
        if (null == storageInfo) {
            return null;
        }
        
        ReplicaInfo replicaInfo = storageInfo.getReplicaInfo();
        byte[] fullState;
        synchronized (storageInfo) {
            fullState = objectStateHandler.extractFullState(key, replicaInfo.getPayload());
        }
        return new ReplicaStorageInfo(replicaInfo, fullState);
    }
    
    public boolean storeReplicaInfo(Object key) {
        return null != retrieveReplicaStorageInfo(key);
    }
    
}
