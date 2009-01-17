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

import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.common.ReplicaStorageInfo;
import org.codehaus.wadi.replication.manager.InternalReplicationManagerException;
import org.codehaus.wadi.replication.manager.ReplicationKeyAlreadyExistsException;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.strategy.BackingStrategy;
import org.codehaus.wadi.servicespace.ServiceInvocationException;
import org.codehaus.wadi.servicespace.ServiceListener;
import org.codehaus.wadi.servicespace.ServiceMonitor;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;

/**
 * 
 * @version $Revision$
 */
public class SyncReplicationManager implements ReplicationManager {
    private final ObjectStateHandler stateHandler;
    private final ReplicaStorage localReplicaStorage;
    private final BackingStrategy backingStrategy;
    private final LocalPeer localPeer;
    private final Map<Object, ReplicaInfo> keyToReplicaInfo;
    private final ServiceMonitor storageMonitor;
    private final ReplicaStorage replicaStorageProxy;
    private final ServiceProxyFactory replicaStorageServiceProxy;
    private final ProxyFactory proxyFactory;
    private final SecondaryManager replicaInfoReOrganizer;


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
        
        replicaStorageServiceProxy = proxyFactory.newReplicaStorageServiceProxyFactory();
        replicaStorageProxy = proxyFactory.newReplicaStorageProxy();

        keyToReplicaInfo = newKeyToReplicaInfo();
        
        replicaInfoReOrganizer = newSecondaryManager();

        storageMonitor = serviceSpace.getServiceMonitor(ReplicaStorage.NAME);
        ServiceListener listener = new ReOrganizeSecondariesListener(backingStrategy, replicaInfoReOrganizer);
        storageMonitor.addServiceLifecycleListener(listener);
    }

    protected SecondaryManager newSecondaryManager() {
        return new SyncSecondaryManager(keyToReplicaInfo,
                backingStrategy,
                localPeer,
                stateHandler,
                replicaStorageServiceProxy);
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
    
    public void create(Object key, Motable tmp) {
        CreateReplicationCommand command = new CreateReplicationCommand(keyToReplicaInfo,
                stateHandler,
                proxyFactory,
                backingStrategy,
                localPeer,
                key,
                tmp);
        command.run();
    }

    public void update(Object key, Motable tmp) {
        UpdateReplicationCommand command = new UpdateReplicationCommand(keyToReplicaInfo,
                stateHandler,
                proxyFactory,
                key,
                tmp);
        command.run();
    }

    public void destroy(Object key) {
        DeleteReplicationCommand command = new DeleteReplicationCommand(keyToReplicaInfo, proxyFactory, key);
        command.run();
    }

    public Motable retrieveReplica(Object key) {
        ReplicaInfo replicaInfo = retrieveReplicaInfo(key);
        if (null == replicaInfo) {
            return null;
        }
        replicaInfo = replicaInfoReOrganizer.updateSecondariesFollowingRestoreFromSecondary(key, replicaInfo);
        return replicaInfo.getPayload();
    }

    public void promoteToMaster(Object key, ReplicaInfo replicaInfo, Motable motable)
            throws InternalReplicationManagerException {
        if (null != replicaInfo) {
            replicaInfo.setPayload(motable);
            insertReplicaInfo(key, replicaInfo);
        } else {
            promoteToMasterWithoutProvidedReplicaInfo(key, motable);
        }
    }
    
    public ReplicaInfo releaseReplicaInfo(Object key, Peer newPrimary) {
        ReplicaInfo replicaInfo;
        synchronized (keyToReplicaInfo) {
            replicaInfo = keyToReplicaInfo.remove(key);
        }
        if (null == replicaInfo) {
            return null;
        }

        Peer[] secondaries = replicaInfo.getSecondaries();
        secondaries = backingStrategy.reElectSecondariesForSwap(key, newPrimary, secondaries);
        replicaInfo = new ReplicaInfo(replicaInfo, newPrimary, secondaries);
        
        for (int i = 0; i < secondaries.length; i++) {
            if (secondaries[i].equals(localPeer)) {
                localReplicaStorage.insert(key, replicaInfo);
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
    
    protected void promoteToMasterWithoutProvidedReplicaInfo(Object key, Motable motable) {
        ReplicaInfo replicaInfo = retrieveReplicaInfo(key);
        if (null == replicaInfo) {
            create(key, motable);
        } else {
            replicaInfo = new ReplicaInfo(replicaInfo, localPeer, replicaInfo.getSecondaries());
            replicaInfoReOrganizer.updateSecondariesFollowingRestoreFromSecondary(key, replicaInfo);
        }
    }

    protected ReplicaInfo retrieveReplicaInfo(Object key) {
        ReplicaStorageInfo storageInfo;
        try {
            storageInfo = replicaStorageProxy.retrieveReplicaStorageInfo(key);
        } catch (ServiceInvocationException e) {
            return null;
        }

        ReplicaInfo replicaInfo = storageInfo.getReplicaInfo();

        Motable target = stateHandler.restoreFromFullStateTransient(key, storageInfo.getSerializedPayload());
        stateHandler.resetObjectState(target);
        replicaInfo.setPayload(target);

        return replicaInfo;
    }
    
    protected void startStorageMonitoring() throws Exception {
        storageMonitor.start();
        Set<Peer> storagePeers = storageMonitor.getHostingPeers();
        backingStrategy.addSecondaries(storagePeers.toArray(new Peer[storagePeers.size()]));
    }
    
    protected void stopStorageMonitoring() throws Exception {
        storageMonitor.stop();
        backingStrategy.reset();
    }
}
