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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.wadi.replication.common.NodeInfo;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.manager.ReplicationManagerStubFactory;
import org.codehaus.wadi.replication.manager.remoting.ReplicationManagerExporter;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.storage.ReplicaStorageStubFactory;
import org.codehaus.wadi.replication.storage.remoting.ReplicaStorageListener;
import org.codehaus.wadi.replication.storage.remoting.ReplicaStorageMonitor;
import org.codehaus.wadi.replication.strategy.BackingStrategy;

/**
 * 
 * @version $Revision$
 */
public class BasicReplicationManager implements ReplicationManager {
    private final Map keyToReplicaInfo;
    private final BackingStrategy backingStrategy;
    private final ReplicationManagerStubFactory managerStubFactory;
    private final ReplicaStorageStubFactory storageStubFactory;
    private final ReplicaStorageMonitor storageMonitor;
    private final ReplicaStorage storage;
    private final ReplicationManagerExporter managerExporter;
    private final NodeInfo primary;
    
    private FilteringStorageListener storageListener;
    
    public BasicReplicationManager(BackingStrategy backingStrategy,
            ReplicationManagerStubFactory managerStubFactory,
            ReplicaStorageStubFactory storageStubFactory,
            ReplicaStorageMonitor storageMonitor,
            ReplicaStorage storage,
            ReplicationManagerExporter managerExporter, 
            NodeInfo primary) {
        this.backingStrategy = backingStrategy;
        this.managerStubFactory = managerStubFactory;
        this.storageStubFactory = storageStubFactory;
        this.storageMonitor = storageMonitor;
        this.storage = storage;
        this.managerExporter = managerExporter;
        this.primary = primary;
        
        keyToReplicaInfo = new HashMap();
    }

    public void start() throws Exception {
        storageListener = new FilteringStorageListener(primary, backingStrategy);
        storageMonitor.addReplicaStorageListener(storageListener);
        storageMonitor.start();
        storage.start();
        managerExporter.export(this);
    }
    
    public void stop() throws Exception {
        managerExporter.unexport(this);
        storage.stop();
        storageMonitor.stop();
        storageMonitor.removeReplicaStorageListener(storageListener);
        keyToReplicaInfo.clear();
    }
    
    public void create(Object key, Object tmp) {
        NodeInfo secondaries[] = backingStrategy.electSecondaries(key);

        ReplicaInfo replicaInfo = new ReplicaInfo(primary, secondaries, tmp);
        synchronized (keyToReplicaInfo) {
            if (keyToReplicaInfo.containsKey(key)) {
                throw new IllegalArgumentException("Key " + key +
                        " is already defined.");
            }
            keyToReplicaInfo.put(key, replicaInfo);
        }

        ReplicaStorage storage = newReplicaStorageStub(secondaries);
        storage.mergeCreate(key, replicaInfo);
    }

    public void update(Object key, Object tmp) {
        ReplicaInfo replicaInfo;
        synchronized (keyToReplicaInfo) {
            replicaInfo = (ReplicaInfo) keyToReplicaInfo.remove(key);
            if (null == replicaInfo) {
                throw new IllegalStateException("Key " + key +
                        " is not defined.");
            }
            replicaInfo = new ReplicaInfo(replicaInfo, tmp);
            keyToReplicaInfo.put(key, replicaInfo);
        }
        // TODO lock on a replicaInfo to ensure that mergeUpdate are properly
        // ordered.
        
        ReplicaStorage storage = newReplicaStorageStub(replicaInfo.getSecondaries());
        storage.mergeUpdate(key, new ReplicaInfo(null, null, tmp));
    }

    public void destroy(Object key) {
        ReplicaInfo replicaInfo;
        synchronized (keyToReplicaInfo) {
            replicaInfo = (ReplicaInfo) keyToReplicaInfo.remove(key);
            if (null == replicaInfo) {
                throw new IllegalStateException("Key " + key +
                        " is not defined.");
            }
        }
        
        ReplicaStorage storage = newReplicaStorageStub(replicaInfo.getSecondaries());
        storage.mergeDestroy(key);
    }

    public Object acquirePrimary(Object key) {
        ReplicationManager service = managerStubFactory.buildStub();
        ReplicaInfo replicaInfo = service.releasePrimary(key);
        
        if (null == replicaInfo) {
            return null;
        }
        
        replicaInfo = reOrganizeSecondaries(key, replicaInfo);

        return replicaInfo.getReplica();
    }

    public ReplicaInfo releasePrimary(Object key) {
        synchronized (keyToReplicaInfo) {
            ReplicaInfo replicaInfo = (ReplicaInfo) keyToReplicaInfo.remove(key);
            if (null == replicaInfo) {
                throw new IllegalArgumentException("Key " + key +
                        " is not defined.");
            }
            return replicaInfo;
        }
        
        // we do not need to inform the secondaries that this manager is
        // no more owning the primary. The manager acquiring the primary
        // will re-organize the secondaries itself.
    }
    
    public ReplicaInfo retrieveReplicaInfo(Object key) {
        synchronized (keyToReplicaInfo) {
            ReplicaInfo replicaInfo = (ReplicaInfo) keyToReplicaInfo.get(key);
            if (null == replicaInfo) {
                throw new IllegalArgumentException("Key " + key +
                        " is not defined.");
            }
            return replicaInfo;
        }
    }
    
    public boolean managePrimary(Object key) {
        synchronized (keyToReplicaInfo) {
            return keyToReplicaInfo.containsKey(key);
        }
    }

    public ReplicaStorage getStorage() {
        return storage;
    }
    
    private void reOrganizeSecondaries() {
        Map tmpKeyToReplicaInfo;
        synchronized (keyToReplicaInfo) {
            tmpKeyToReplicaInfo = new HashMap(keyToReplicaInfo);
        }
        // TODO make that a strategy
        for (Iterator iter = tmpKeyToReplicaInfo.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            Object key = entry.getKey();
            ReplicaInfo replicaInfo = (ReplicaInfo) entry.getValue();
            reOrganizeSecondaries(key, replicaInfo);
        }
    }
    
    private ReplicaInfo reOrganizeSecondaries(Object key, ReplicaInfo replicaInfo) {
        NodeInfo oldSecondaries[] = replicaInfo.getSecondaries();
        NodeInfo newSecondaries[] = backingStrategy.reElectSecondaries(key, 
                replicaInfo.getPrimary(), 
                oldSecondaries);

        replicaInfo = new ReplicaInfo(replicaInfo, primary, newSecondaries);

        synchronized (keyToReplicaInfo) {
            keyToReplicaInfo.put(key, replicaInfo);
        }

        updateReplicaStorages(key, replicaInfo, oldSecondaries, newSecondaries);
        return replicaInfo;
    }

    private ReplicaStorage newReplicaStorageStub(NodeInfo[] nodes) {
        return storageStubFactory.buildStub(nodes);
    }
    
    private void updateReplicaStorages(Object key, ReplicaInfo replicaInfo, NodeInfo[] oldSecondaries, NodeInfo[] newSecondaries) {
        StorageCommandBuilder commandBuilder = new StorageCommandBuilder(
                key,
                replicaInfo,
                oldSecondaries);
        
        StorageCommand[] commands = commandBuilder.build();
        for (int i = 0; i < commands.length; i++) {
            StorageCommand command = commands[i];
            command.execute(storageStubFactory);
        }
    }
    
    private class FilteringStorageListener implements ReplicaStorageListener {
        private final NodeInfo primary;
        private final BackingStrategy backingStrategy;
        
        public FilteringStorageListener(NodeInfo primary, BackingStrategy backingStrategy) {
            this.primary = primary;
            this.backingStrategy = backingStrategy;
        }

        public void initNodes(NodeInfo[] nodes) {
            List filteredNodes = Arrays.asList(nodes);
            filteredNodes.remove(primary);
            NodeInfo newNodes[] = (NodeInfo[]) filteredNodes.toArray(new NodeInfo[0]);
            
            backingStrategy.addSecondaries(newNodes);
        }
        
        public void fireJoin(NodeInfo node) {
            if (node.equals(primary)) {
                return;
            }
            
            backingStrategy.addSecondary(node);
            reOrganizeSecondaries();
        }

        public void fireLeave(NodeInfo node) {
            if (node.equals(primary)) {
                return;
            }

            backingStrategy.removeSecondary(node);
            reOrganizeSecondaries();
        }
    }
}
