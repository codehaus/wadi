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

import org.codehaus.wadi.replication.common.NodeInfo;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.storage.remoting.ReplicaStorageExporter;

/**
 * 
 * @version $Revision$
 */
public class MemoryReplicaStorage implements ReplicaStorage {
    private final NodeInfo hostingNode;
    private final Map keyToReplica;
    private final ReplicaStorageExporter storageExporter;
    
    public MemoryReplicaStorage(ReplicaStorageExporter storageExporter,
            NodeInfo hostingNode) {
        this.storageExporter = storageExporter;
        this.hostingNode = hostingNode;
        
        keyToReplica = new HashMap();
    }

    public void start() throws Exception {
        storageExporter.export(this);
    }

    public void stop() throws Exception {
        storageExporter.unexport(this);
        keyToReplica.clear();
    }
    
    public void mergeCreate(Object key, ReplicaInfo replicaInfo) {
        synchronized (keyToReplica) {
            if (keyToReplica.containsKey(key)) {
                throw new IllegalArgumentException("Key " + key + 
                        " is already defined by " + hostingNode);
            }
            keyToReplica.put(key, replicaInfo);
        }
    }

    public void mergeUpdate(Object key, ReplicaInfo replicaInfo) {
        synchronized (keyToReplica) {
            ReplicaInfo curReplicaInfo = getRequiredReplicaInfo(key);
            replicaInfo = new ReplicaInfo(curReplicaInfo, replicaInfo);
            keyToReplica.put(key, replicaInfo);
        }
    }

    public void mergeDestroy(Object key) {
        synchronized (keyToReplica) {
            Object object = keyToReplica.remove(key);
            if (null == object) {
                throw new IllegalArgumentException("Key " + key +
                        " is not defined by " + hostingNode);
            }
        }
    }
    
    public ReplicaInfo retrieveReplicaInfo(Object key) {
        synchronized (keyToReplica) {
            ReplicaInfo replicaInfo = getRequiredReplicaInfo(key);
            return replicaInfo;
        }
    }

    public boolean storeReplicaInfo(Object key) {
        synchronized (keyToReplica) {
            return keyToReplica.containsKey(key);
        }
    }
    
    public NodeInfo getHostingNode() {
        return hostingNode;
    }
    
    private ReplicaInfo getRequiredReplicaInfo(Object key) {
        ReplicaInfo replicaInfo = (ReplicaInfo) keyToReplica.get(key);
        if (null == replicaInfo) {
            throw new IllegalArgumentException("Key " + key +
                    " is not defined by " + hostingNode);
        }
        return replicaInfo;
    }
}
