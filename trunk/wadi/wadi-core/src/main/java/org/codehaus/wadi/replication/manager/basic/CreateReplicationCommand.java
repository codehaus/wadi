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

import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.common.ReplicaStorageInfo;
import org.codehaus.wadi.replication.manager.InternalReplicationManagerException;
import org.codehaus.wadi.replication.manager.ReplicationKeyAlreadyExistsException;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.strategy.BackingStrategy;
import org.codehaus.wadi.servicespace.ServiceInvocationException;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class CreateReplicationCommand implements Runnable {
    private final Map<Object, ReplicaInfo> keyToReplicaInfo;
    private final ObjectStateHandler stateHandler;
    private final ProxyFactory proxyFactory;
    private final BackingStrategy backingStrategy;
    private final LocalPeer localPeer;
    private final Object key;
    private final Motable payload;

    public CreateReplicationCommand(Map<Object, ReplicaInfo> keyToReplicaInfo,
            ObjectStateHandler stateHandler,
            ProxyFactory proxyFactory,
            BackingStrategy backingStrategy,
            LocalPeer localPeer,
            Object key,
            Motable payload) {
        if (null == keyToReplicaInfo) {
            throw new IllegalArgumentException("keyToReplicaInfo is required");
        } else if (null == stateHandler) {
            throw new IllegalArgumentException("stateHandler is required");
        } else if (null == proxyFactory) {
            throw new IllegalArgumentException("proxyFactory is required");
        } else if (null == backingStrategy) {
            throw new IllegalArgumentException("backingStrategy is required");
        } else if (null == localPeer) {
            throw new IllegalArgumentException("localPeer is required");
        } else if (null == key) {
            throw new IllegalArgumentException("key is required");
        } else if (null == payload) {
            throw new IllegalArgumentException("payload is required");
        }
        this.keyToReplicaInfo = keyToReplicaInfo;
        this.stateHandler = stateHandler;
        this.proxyFactory = proxyFactory;
        this.backingStrategy = backingStrategy;
        this.localPeer = localPeer;
        this.key = key;
        this.payload = payload;
    }

    public void run() {
        synchronized (keyToReplicaInfo) {
            if (keyToReplicaInfo.containsKey(key)) {
                throw new ReplicationKeyAlreadyExistsException(key);
            }
        }

        byte[] fullState = stateHandler.extractFullState(key, payload);
        stateHandler.resetObjectState(payload);
        
        CreateReplicaTask backOffCapableTask = new CreateReplicaTask(key, payload, fullState);
        backOffCapableTask.attempt();
    }
    
    protected void cascadeCreate(Object key, ReplicaInfo replicaInfo, byte[] fullState, BackOffCapableTask task) {
        ReplicaStorage storage = proxyFactory.newReplicaStorageProxy(replicaInfo.getSecondaries());
        try {
            storage.mergeCreate(key, new ReplicaStorageInfo(replicaInfo, fullState));
            task.complete();
        } catch (ServiceInvocationException e) {
            if (e.isMessageExchangeException()) {
                task.backoff();
            } else {
                throw e;
            }
        }
    }
    
    protected interface BackOffCapableTask {
        void attempt();
        
        void backoff();
        
        void complete();
    }
    
    protected class CreateReplicaTask implements BackOffCapableTask {
        private static final int NB_ATTEMPT = 4;
        private static final long BACK_OFF_PERIOD = 1000;

        protected final Object key;
        private final Motable tmp;
        private final byte[] fullState;
        private volatile int currentAttempt;
        private volatile ReplicaInfo replicaInfo;

        private CreateReplicaTask(Object key, Motable tmp, byte[] fullState) {
            this.key = key;
            this.tmp = tmp;
            this.fullState = fullState;
        }

        public void backoff() {
            if (currentAttempt == NB_ATTEMPT) {
                throw new InternalReplicationManagerException("Backoff failure for key [" + key + "]");
            }
            try {
                Thread.sleep(BACK_OFF_PERIOD);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InternalReplicationManagerException("Backoff cancelled");
            }
            attempt();
        }
        
        public void attempt() {
            currentAttempt++;
            doAttempt();
        }

        public void doAttempt() {
            Peer secondaries[] = backingStrategy.electSecondaries(key);
            if (null == replicaInfo) {
                replicaInfo = new ReplicaInfo(localPeer, secondaries, tmp);
            } else {
                replicaInfo.updateSecondaries(secondaries);
            }
            if (secondaries.length != 0) {
                cascadeCreate(key, replicaInfo, fullState, this);
            } else {
                complete();
            }
        }

        public void complete() {
            synchronized (keyToReplicaInfo) {
                keyToReplicaInfo.put(key, replicaInfo);
            }
        }

    }
    
}
