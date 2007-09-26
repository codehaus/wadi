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
package org.codehaus.wadi.replication.manager.basic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.common.ReplicaStorageInfo;
import org.codehaus.wadi.replication.manager.InternalReplicationManagerException;
import org.codehaus.wadi.replication.manager.ReplicationKeyAlreadyExistsException;
import org.codehaus.wadi.replication.manager.ReplicationKeyNotFoundException;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.strategy.BackingStrategy;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceInvocationException;
import org.codehaus.wadi.servicespace.ServiceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceListener;
import org.codehaus.wadi.servicespace.ServiceMonitor;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;

/**
 * 
 * @version $Revision$
 */
public class BasicReplicationManager implements ReplicationManager {
    private static final Log log = LogFactory.getLog(BasicReplicationManager.class);
    
    private final ObjectStateHandler stateHandler;
    private final BackingStrategy backingStrategy;
    private final LocalPeer localPeer;
    private final Map<Object, ReplicaInfo> keyToReplicaInfo;
    private final ServiceMonitor storageMonitor;
    private final ReplicationManager replicationManagerProxy;
    private final ReplicaStorage replicaStorageProxy;
    private final ServiceProxyFactory replicaStorageServiceProxy;
    private final ProxyFactory proxyFactory;

    public BasicReplicationManager(ServiceSpace serviceSpace,
        ObjectStateHandler stateHandler,
        BackingStrategy backingStrategy) {
        this(serviceSpace, stateHandler, backingStrategy, new BasicProxyFactory(serviceSpace));
    }    
    
    public BasicReplicationManager(ServiceSpace serviceSpace,
            ObjectStateHandler stateHandler,
            BackingStrategy backingStrategy,
            ProxyFactory proxyFactory) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == stateHandler) {
            throw new IllegalArgumentException("stateHandler is required");
        } else if (null == backingStrategy) {
            throw new IllegalArgumentException("backingStrategy is required");
        } else if (null == proxyFactory) {
            throw new IllegalArgumentException("proxyFactory is required");
        }
        this.stateHandler = stateHandler;
        this.backingStrategy = backingStrategy;
        this.proxyFactory = proxyFactory;
        
        localPeer = serviceSpace.getLocalPeer();

        storageMonitor = serviceSpace.getServiceMonitor(ReplicaStorage.NAME);
        storageMonitor.addServiceLifecycleListener(new UpdateBackingStrategyListener(backingStrategy));
        
        replicationManagerProxy = proxyFactory.newReplicationManagerProxy();
        replicaStorageServiceProxy = proxyFactory.newReplicaStorageServiceProxyFactory();
        replicaStorageProxy = proxyFactory.newReplicaStorageProxy();

        keyToReplicaInfo = new HashMap<Object, ReplicaInfo>();
    }

    public void start() throws Exception {
        startStorageMonitoring();
    }

    public void stop() throws Exception {
        synchronized (keyToReplicaInfo) {
            keyToReplicaInfo.clear();
        }
        stopStorageMonitoring();
    }
    
    public void create(Object key, Object tmp) {
        synchronized (keyToReplicaInfo) {
            if (keyToReplicaInfo.containsKey(key)) {
                throw new ReplicationKeyAlreadyExistsException(key);
            }
        }

        byte[] fullState = stateHandler.extractFullState(key, tmp);
        stateHandler.resetObjectState(tmp);
        
        CreateReplicaTask backOffCapableTask = new CreateReplicaTask(key, tmp, fullState);
        backOffCapableTask.attempt();
    }

    public void update(Object key, Object tmp) {
        ReplicaInfo replicaInfo;
        synchronized (keyToReplicaInfo) {
            replicaInfo = keyToReplicaInfo.get(key);
        }
        if (null == replicaInfo) {
            throw new ReplicationKeyNotFoundException(key);
        }
        
        byte[] updatedState = stateHandler.extractUpdatedState(key, tmp);
        stateHandler.resetObjectState(tmp);
        
        replicaInfo.increaseVersion();
        
        if (replicaInfo.getSecondaries().length != 0) {
            cascadeUpdate(key, replicaInfo, updatedState);
        }
    }

    public void destroy(Object key) {
        ReplicaInfo replicaInfo;
        synchronized (keyToReplicaInfo) {
            replicaInfo = keyToReplicaInfo.remove(key);
        }
        if (null == replicaInfo) {
            log.warn("Key [" + key + "] is not defined; cannot destroy it.");
            return;
        }
        
        if (replicaInfo.getSecondaries().length != 0) {
            cascadeDestroy(key, replicaInfo);
        }
    }
    
    public Object retrieveReplica(Object key) {
        return retrieveOrCreateReplica(key, null);
    }

    public void acquirePrimary(Object key, Object tmp) {
        retrieveOrCreateReplica(key, tmp);
    }

    protected Object retrieveOrCreateReplica(Object key, Object tmp) {
        ReplicaInfo replicaInfo;
        try {
            replicationManagerProxy.releasePrimary(key);
            ReplicaStorageInfo storageInfo = replicaStorageProxy.retrieveReplicaStorageInfo(key);
            
            replicaInfo = storageInfo.getReplicaInfo();
            if (null == tmp) {
                Object target = stateHandler.restoreFromFullStateTransient(key, storageInfo.getSerializedPayload());
                stateHandler.resetObjectState(target);
                replicaInfo.setPayload(target);
            } else {
                replicaInfo.setPayload(tmp);
            }
        } catch (ServiceInvocationException e) {
            if (e.isMessageExchangeException()) {
                if (null == tmp) {
                    return null;
                }
                synchronized (keyToReplicaInfo) {
                    replicaInfo = new ReplicaInfo(localPeer, new Peer[0], tmp);
                    keyToReplicaInfo.put(key, replicaInfo);
                }
            } else {
                throw new ReplicationKeyNotFoundException(key, e);
            }
        }
        
        replicaInfo = reOrganizeSecondaries(key, replicaInfo);
        return replicaInfo.getPayload();
    }

    public boolean releasePrimary(Object key) {
        Object object;
        synchronized (keyToReplicaInfo) {
            object = keyToReplicaInfo.remove(key);
        }
        // we do not need to inform the secondaries that this manager is
        // no more owning the primary. The manager acquiring the primary
        // will re-organize the secondaries itself.
        return object != null;
    }
    
    protected void cascadeCreate(Object key, ReplicaInfo replicaInfo, byte[] updatedState, BackOffCapableTask task) {
        ReplicaStorage storage = proxyFactory.newReplicaStorageProxy(replicaInfo.getSecondaries());
        try {
            storage.mergeCreate(key, new ReplicaStorageInfo(replicaInfo, updatedState));
            task.complete();
        } catch (ServiceInvocationException e) {
            if (e.isMessageExchangeException()) {
                task.backoff();
            } else {
                throw e;
            }
        }
    }
    
    protected void cascadeUpdate(Object key, ReplicaInfo replicaInfo, byte[] updatedState) {
        ReplicaStorage storage = proxyFactory.newReplicaStorageProxy(replicaInfo.getSecondaries());
        try {
            storage.mergeUpdate(key, new ReplicaStorageInfo(replicaInfo, updatedState));
        } catch (ServiceInvocationException e) {
            if (e.isMessageExchangeException()) {
                log.warn("Update has not been properly cascaded due to a communication failure. If a targeted node " +
                        "has been lost, state will be re-balanced automatically.", e);
            } else {
                throw new InternalReplicationManagerException(e);
            }
        }
    }
    
    protected void cascadeDestroy(Object key, ReplicaInfo replicaInfo) {
        ReplicaStorage storage = proxyFactory.newReplicaStorageProxyForDelete(replicaInfo.getSecondaries());
        storage.mergeDestroy(key);
    }

    protected void reOrganizeSecondaries() {
        Map<Object, ReplicaInfo> tmpKeyToReplicaInfo;
        synchronized (keyToReplicaInfo) {
            tmpKeyToReplicaInfo = new HashMap<Object, ReplicaInfo>(keyToReplicaInfo);
        }
        for (Map.Entry<Object, ReplicaInfo> entry : tmpKeyToReplicaInfo.entrySet()) {
            Object key = entry.getKey();
            ReplicaInfo replicaInfo = entry.getValue();
            reOrganizeSecondaries(key, replicaInfo);
        }
    }
    
    protected ReplicaInfo reOrganizeSecondaries(Object key, ReplicaInfo replicaInfo) {
        Peer oldSecondaries[] = replicaInfo.getSecondaries();
        Peer newSecondaries[] = backingStrategy.reElectSecondaries(key, replicaInfo.getPrimary(), oldSecondaries);
        replicaInfo = new ReplicaInfo(replicaInfo, localPeer, newSecondaries);
        synchronized (keyToReplicaInfo) {
            keyToReplicaInfo.put(key, replicaInfo);
        }
        
        updateReplicaStorages(key, replicaInfo, oldSecondaries);
        return replicaInfo;
    }

    protected void updateReplicaStorages(Object key, ReplicaInfo replicaInfo, Peer[] oldSecondaries) {
        StorageCommandBuilder commandBuilder = new StorageCommandBuilder(key, replicaInfo, oldSecondaries, stateHandler);
        StorageCommand[] commands = commandBuilder.build();
        for (int i = 0; i < commands.length; i++) {
            StorageCommand command = commands[i];
            command.execute(replicaStorageServiceProxy);
        }
    }
    
    protected void startStorageMonitoring() throws Exception {
        storageMonitor.start();
        Set storagePeers = storageMonitor.getHostingPeers();
        backingStrategy.addSecondaries((Peer[]) storagePeers.toArray(new Peer[storagePeers.size()]));
    }
    
    protected void stopStorageMonitoring() throws Exception {
        storageMonitor.stop();
        backingStrategy.reset();
    }
    
    protected class UpdateBackingStrategyListener implements ServiceListener {
        private final BackingStrategy backingStrategy;
        
        public UpdateBackingStrategyListener(BackingStrategy backingStrategy) {
            this.backingStrategy = backingStrategy;
        }

        public void receive(ServiceLifecycleEvent event, Set newHostingPeers) {
            LifecycleState state = event.getState();
            if (state == LifecycleState.AVAILABLE || state == LifecycleState.STARTED) {
                backingStrategy.addSecondary(event.getHostingPeer());
                reOrganizeSecondaries();
            } else if (state == LifecycleState.STOPPING || state == LifecycleState.FAILED) {
                backingStrategy.removeSecondary(event.getHostingPeer());
                reOrganizeSecondaries();
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
        private final Object tmp;
        private final byte[] updatedState;
        private volatile int currentAttempt;
        private volatile ReplicaInfo replicaInfo;

        private CreateReplicaTask(Object key, Object tmp, byte[] updatedState) {
            this.key = key;
            this.tmp = tmp;
            this.updatedState = updatedState;
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
                cascadeCreate(key, replicaInfo, updatedState, this);
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
