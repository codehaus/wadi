/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.replication.manager.basic;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.storage.ReplicaStorage;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class DeleteReplicationCommand implements Runnable {
    private static final Log LOG = LogFactory.getLog(DeleteReplicationCommand.class);
    
    private final Map<Object, ReplicaInfo> keyToReplicaInfo;
    private final ProxyFactory proxyFactory;
    private final Object key;

    public DeleteReplicationCommand(Map<Object, ReplicaInfo> keyToReplicaInfo, ProxyFactory proxyFactory, Object key) {
        if (null == keyToReplicaInfo) {
            throw new IllegalArgumentException("keyToReplicaInfo is required");
        } else if (null == proxyFactory) {
            throw new IllegalArgumentException("proxyFactory is required");
        } else if (null == key) {
            throw new IllegalArgumentException("key is required");
        }
        this.key = key;
        this.keyToReplicaInfo = keyToReplicaInfo;
        this.proxyFactory = proxyFactory;
    }

    public void run() {
        ReplicaInfo replicaInfo;
        synchronized (keyToReplicaInfo) {
            replicaInfo = keyToReplicaInfo.remove(key);
        }
        if (null == replicaInfo) {
            LOG.warn("Key [" + key + "] is not defined; cannot destroy it.");
            return;
        }
        
        if (replicaInfo.getSecondaries().length != 0) {
            cascadeDestroy(key, replicaInfo);
        }
    }

    protected void cascadeDestroy(Object key, ReplicaInfo replicaInfo) {
        ReplicaStorage storage = proxyFactory.newReplicaStorageProxyForDelete(replicaInfo.getSecondaries());
        storage.mergeDestroy(key);
    }

}
