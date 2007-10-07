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
import java.util.HashSet;
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
public class SyncReplicationManager implements ReplicationManager {
    private static final Log log = LogFactory.getLog(SyncReplicationManager.class);
    
    private final ObjectStateHandler stateHandler;
    private final ReplicaStorage localReplicaStorage;
    private final BackingStrategy backingStrategy;
    private final LocalPeer localPeer;
    private final Map<Object, ReplicaInfo> keyToReplicaInfo;
    private final ServiceMonitor storageMonitor;
    private final ReplicaStorage replicaStorageProxy;
    private final ServiceProxyFactory replicaStorageServiceProxy;
    private final ProxyFactory proxyFactory;


    public SyncReplicationManager(ServiceSpace serviceSpace,
        ObjectStateHandler stateHandler,
        BackingStrategy backingStrategy,
        ReplicaStorage localReplicaStorage) {
        this(serviceSpace, stateHandler, backingStrategy, localReplicaStorage, new BasicProxyFactory(serviceSpace));
    }    
    
    public SyncReplicationManager(ServiceSpace serviceSpace,
            ObjectStateHandler stateHandler,
            BackingStrategy backingStrategy,
            ReplicaStorage localReplicaStorage,
            ProxyFactory proxyFactory) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == stateHandler) {
            throw new IllegalArgumentException("stateHandler is required");
        } else if (null == backingStrategy) {
            throw new IllegalArgumentException("backingStrategy is required");
        } else if (null == localReplicaStorage) {
            throw new IllegalArgumentException("localReplicaStorage is required");
        } else if (null == proxyFactory) {
            throw new IllegalArgumentException("proxyFactory is required");
        }
        this.stateHandler = stateHandler;
        this.backingStrategy = backingStrategy;
        this.localReplicaStorage = localReplicaStorage;
        this.proxyFactory = proxyFactory;
        
        localPeer = serviceSpace.getLocalPeer();

        storageMonitor = serviceSpace.getServiceMonitor(ReplicaStorage.NAME);
        storageMonitor.addServiceLifecycleListener(new UpdateBackingStrategyListener(backingStrategy));
        
        replicaStorageServiceProxy = proxyFactory.newReplicaStorageServiceProxyFactory();
        replicaStorageProxy = proxyFactory.newReplicaStorageProxy();

        keyToReplicaInfo = newKeyToReplicaInfo();
    }

    protected Map<Object, ReplicaInfo> newKeyToReplicaInfo() {
        return new HashMap<Object, ReplicaInfo>();
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
        ReplicaInfo replicaInfo;
        try {
            ReplicaStorageInfo storageInfo = replicaStorageProxy.retrieveReplicaStorageInfo(key);
            
            replicaInfo = storageInfo.getReplicaInfo();
            Object target = stateHandler.restoreFromFullStateTransient(key, storageInfo.getSerializedPayload());
            stateHandler.resetObjectState(target);
            replicaInfo.setPayload(target);
        } catch (ServiceInvocationException e) {
            if (e.isMessageExchangeException()) {
                return null;
            } else {
                throw new ReplicationKeyNotFoundException(key, e);
            }
        }

        replicaInfo = reOrganizeSecondaries(key, replicaInfo);
        return replicaInfo.getPayload();
    }

    public ReplicaInfo releaseReplicaInfo(Object key, Peer newPrimary) throws ReplicationKeyNotFoundException {
        ReplicaInfo replicaInfo;
        synchronized (keyToReplicaInfo) {
            replicaInfo = keyToReplicaInfo.remove(key);
        }
        if (null == replicaInfo) {
            throw new ReplicationKeyNotFoundException(key);
        }

        Peer[] secondaries = replicaInfo.getSecondaries();
        secondaries = backingStrategy.reElectSecondariesForSwap(key, newPrimary, secondaries);
        replicaInfo = new ReplicaInfo(replicaInfo, newPrimary, secondaries);
        
        for (int i = 0; i < secondaries.length; i++) {
            if (secondaries[i].equals(localPeer)) {
                byte[] fullState = stateHandler.extractFullState(key, replicaInfo.getPayload());
                localReplicaStorage.insert(key, new ReplicaStorageInfo(replicaInfo, fullState));
                break;
            }
        }
        
        return replicaInfo;
    }
    
    public void insertReplicaInfo(Object key, ReplicaInfo replicaInfo) throws ReplicationKeyAlreadyExistsException {
        synchronized (keyToReplicaInfo) {
            if (keyToReplicaInfo.containsKey(key)) {
                throw new ReplicationKeyAlreadyExistsException(key);
            }
            keyToReplicaInfo.put(key, replicaInfo);
        }
        localReplicaStorage.mergeDestroyIfExist(key);
    }

    public Set<Object> getManagedReplicaInfoKeys() {
        synchronized (keyToReplicaInfo) {
            return new HashSet<Object>(keyToReplicaInfo.keySet());
        }
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
        private final byte[] fullState;
        private volatile int currentAttempt;
        private volatile ReplicaInfo replicaInfo;

        private CreateReplicaTask(Object key, Object tmp, byte[] fullState) {
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
