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
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.common.ReplicaStorageInfo;
import org.codehaus.wadi.replication.manager.InternalReplicationManagerException;
import org.codehaus.wadi.replication.manager.ReplicationKeyNotFoundException;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.servicespace.ServiceInvocationException;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class UpdateReplicationCommand implements Runnable {
    private static final Log LOG = LogFactory.getLog(UpdateReplicationCommand.class);
    
    private final Map<Object, ReplicaInfo> keyToReplicaInfo;
    private final ObjectStateHandler stateHandler;
    private final ProxyFactory proxyFactory;
    private final Object key;
    private final Motable payload;

    public UpdateReplicationCommand(Map<Object, ReplicaInfo> keyToReplicaInfo,
            ObjectStateHandler stateHandler,
            ProxyFactory proxyFactory,
            Object key,
            Motable payload) {
        if (null == keyToReplicaInfo) {
            throw new IllegalArgumentException("keyToReplicaInfo is required");
        } else if (null == stateHandler) {
            throw new IllegalArgumentException("stateHandler is required");
        } else if (null == proxyFactory) {
            throw new IllegalArgumentException("proxyFactory is required");
        } else if (null == key) {
            throw new IllegalArgumentException("key is required");
        } else if (null == payload) {
            throw new IllegalArgumentException("payload is required");
        }
        this.keyToReplicaInfo = keyToReplicaInfo;
        this.stateHandler = stateHandler;
        this.proxyFactory = proxyFactory;
        this.key = key;
        this.payload = payload;
    }

    public void run() {
        ReplicaInfo replicaInfo;
        synchronized (keyToReplicaInfo) {
            replicaInfo = keyToReplicaInfo.get(key);
        }
        if (null == replicaInfo) {
            throw new ReplicationKeyNotFoundException(key);
        }
        
        replicaInfo.setPayload(payload);
        byte[] updatedState = stateHandler.extractUpdatedState(key, payload);
        stateHandler.resetObjectState(payload);
        
        replicaInfo.increaseVersion();
        
        if (replicaInfo.getSecondaries().length != 0) {
            cascadeUpdate(key, replicaInfo, updatedState);
        }
    }

    protected void cascadeUpdate(Object key, ReplicaInfo replicaInfo, byte[] updatedState) {
        ReplicaStorage storage = proxyFactory.newReplicaStorageProxy(replicaInfo.getSecondaries());
        try {
            storage.mergeUpdate(key, new ReplicaStorageInfo(replicaInfo, updatedState));
        } catch (ServiceInvocationException e) {
            if (e.isMessageExchangeException()) {
                LOG.warn("Update has not been properly cascaded due to a communication failure. If a targeted node " +
                        "has been lost, state will be re-balanced automatically.", e);
            } else {
                throw new InternalReplicationManagerException(e);
            }
        }
    }

}
