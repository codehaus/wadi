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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.strategy.BackingStrategy;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceListener;
import org.codehaus.wadi.servicespace.ServiceMonitor;
import org.codehaus.wadi.servicespace.ServiceProxy;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;

/**
 * 
 * @version $Revision$
 */
public class BasicReplicationManager implements ReplicationManager {
    private final ServiceSpace serviceSpace;
    private final LocalPeer localPeer;
    private final Map keyToReplicaInfo;
    private final BackingStrategy backingStrategy;
    private final ServiceMonitor storageMonitor;
    private final boolean aSyncReplication;
    private final ReplicationManager replicationManagerProxy;
    private final ReplicaStorage replicaStorageProxy;
    private final ServiceProxyFactory replicaStorageServiceProxy;
    
    public BasicReplicationManager(ServiceSpace serviceSpace,
            BackingStrategy backingStrategy,
            boolean aSyncReplication) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == backingStrategy) {
            throw new IllegalArgumentException("backingStrategy is required");
        }
        this.serviceSpace = serviceSpace;
        this.backingStrategy = backingStrategy;
        this.aSyncReplication = aSyncReplication;
        
        localPeer = serviceSpace.getLocalPeer();

        storageMonitor = serviceSpace.getServiceMonitor(ReplicaStorage.NAME);
        storageMonitor.addServiceLifecycleListener(new UpdateBackingStrategyListener(backingStrategy));
        
        replicationManagerProxy = newReplicationManagerProxy(serviceSpace);
        replicaStorageServiceProxy = serviceSpace.getServiceProxyFactory(
                        ReplicaStorage.NAME, new Class[] {ReplicaStorage.class});
        replicaStorageProxy = (ReplicaStorage) replicaStorageServiceProxy.getProxy();

        keyToReplicaInfo = new HashMap();
    }

    private ReplicationManager newReplicationManagerProxy(ServiceSpace serviceSpace) {
        ServiceProxyFactory repManagerProxyFactory = serviceSpace.getServiceProxyFactory(ReplicationManager.NAME, 
                new Class[] {ReplicationManager.class});
        repManagerProxyFactory.getInvocationMetaData().setOneWay(true);
        return (ReplicationManager) repManagerProxyFactory.getProxy();
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
        Peer secondaries[] = backingStrategy.electSecondaries(key);

        ReplicaInfo replicaInfo = new ReplicaInfo(localPeer, secondaries, tmp);
        synchronized (keyToReplicaInfo) {
            if (keyToReplicaInfo.containsKey(key)) {
                throw new IllegalArgumentException("Key [" + key + "] is already defined.");
            }
            keyToReplicaInfo.put(key, replicaInfo);
        }

        if (secondaries.length != 0) {
            ReplicaStorage storage = newReplicaStorageStub(secondaries);
            storage.mergeCreate(key, replicaInfo);
        }
    }

    public void update(Object key, Object tmp) {
        ReplicaInfo replicaInfo;
        synchronized (keyToReplicaInfo) {
            replicaInfo = (ReplicaInfo) keyToReplicaInfo.remove(key);
            if (null == replicaInfo) {
                throw new IllegalStateException("Key [" + key + "] is not defined.");
            }
            replicaInfo = new ReplicaInfo(replicaInfo, tmp);
            keyToReplicaInfo.put(key, replicaInfo);
        }
        
        if (replicaInfo.getSecondaries().length != 0) {
            ReplicaStorage storage = newReplicaStorageStub(replicaInfo.getSecondaries());
            storage.mergeUpdate(key, new ReplicaInfo(tmp));
        }
    }

    public void destroy(Object key) {
        ReplicaInfo replicaInfo;
        synchronized (keyToReplicaInfo) {
            replicaInfo = (ReplicaInfo) keyToReplicaInfo.remove(key);
            if (null == replicaInfo) {
                throw new IllegalStateException("Key [" + key + "] is not defined.");
            }
        }
        
        if (replicaInfo.getSecondaries().length != 0) {
            ReplicaStorage storage = newReplicaStorageStub(replicaInfo.getSecondaries());
            storage.mergeDestroy(key);
        }
    }

    public Object acquirePrimary(Object key) {
        replicationManagerProxy.releasePrimary(key);

        ReplicaInfo replicaInfo = replicaStorageProxy.retrieveReplicaInfo(key);
        if (null == replicaInfo) {
            throw new IllegalStateException("Cannot acquire primary");
        }
        
        replicaInfo = reOrganizeSecondaries(key, replicaInfo);
        return replicaInfo.getReplica();
    }

    public void releasePrimary(Object key) {
        synchronized (keyToReplicaInfo) {
            keyToReplicaInfo.remove(key);
        }
        // we do not need to inform the secondaries that this manager is
        // no more owning the primary. The manager acquiring the primary
        // will re-organize the secondaries itself.
    }
    
    public ReplicaInfo retrieveReplicaInfo(Object key) {
        synchronized (keyToReplicaInfo) {
            ReplicaInfo replicaInfo = (ReplicaInfo) keyToReplicaInfo.get(key);
            if (null == replicaInfo) {
                throw new IllegalArgumentException("Key [" + key + "] is not defined.");
            }
            return replicaInfo;
        }
    }
    
    public boolean managePrimary(Object key) {
        synchronized (keyToReplicaInfo) {
            return keyToReplicaInfo.containsKey(key);
        }
    }

    protected void reOrganizeSecondaries() {
        Map tmpKeyToReplicaInfo;
        synchronized (keyToReplicaInfo) {
            tmpKeyToReplicaInfo = new HashMap(keyToReplicaInfo);
        }
        for (Iterator iter = tmpKeyToReplicaInfo.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            Object key = entry.getKey();
            ReplicaInfo replicaInfo = (ReplicaInfo) entry.getValue();
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

    protected ReplicaStorage newReplicaStorageStub(Peer[] peers) {
        ServiceProxy serviceProxy = (ServiceProxy) replicaStorageServiceProxy.getProxy();
        serviceProxy.getInvocationMetaData().setTargets(peers);
        return (ReplicaStorage) serviceProxy;
    }
    
    protected void updateReplicaStorages(Object key, ReplicaInfo replicaInfo, Peer[] oldSecondaries) {
        StorageCommandBuilder commandBuilder = new StorageCommandBuilder(key, replicaInfo, oldSecondaries);
        StorageCommand[] commands = commandBuilder.build();
        ServiceProxyFactory proxyFactory = serviceSpace.getServiceProxyFactory(
                ReplicaStorage.NAME, new Class[] {ReplicaStorage.class});
        for (int i = 0; i < commands.length; i++) {
            StorageCommand command = commands[i];
            command.execute(proxyFactory);
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

        public void receive(ServiceLifecycleEvent event) {
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
    
}
