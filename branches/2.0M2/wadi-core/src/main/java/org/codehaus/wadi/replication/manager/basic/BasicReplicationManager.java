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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
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
import org.codehaus.wadi.servicespace.ServiceProxy;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.replyaccessor.DoNotReplyWithNull;

/**
 * 
 * @version $Revision$
 */
public class BasicReplicationManager implements ReplicationManager {
    private static final Log log = LogFactory.getLog(BasicReplicationManager.class);
    
    private final ServiceSpace serviceSpace;
    private final LocalPeer localPeer;
    private final Map keyToReplicaInfo;
    private final BackingStrategy backingStrategy;
    private final ServiceMonitor storageMonitor;
    private final ReplicationManager replicationManagerProxy;
    private final ReplicaStorage replicaStorageProxy;
    private final ServiceProxyFactory replicaStorageServiceProxy;
    
    public BasicReplicationManager(ServiceSpace serviceSpace, BackingStrategy backingStrategy) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == backingStrategy) {
            throw new IllegalArgumentException("backingStrategy is required");
        }
        this.serviceSpace = serviceSpace;
        this.backingStrategy = backingStrategy;
        
        localPeer = serviceSpace.getLocalPeer();

        storageMonitor = serviceSpace.getServiceMonitor(ReplicaStorage.NAME);
        storageMonitor.addServiceLifecycleListener(new UpdateBackingStrategyListener(backingStrategy));
        
        replicationManagerProxy = newReplicationManagerProxy(serviceSpace);
        replicaStorageServiceProxy = serviceSpace.getServiceProxyFactory(
                        ReplicaStorage.NAME, new Class[] {ReplicaStorage.class});
        replicaStorageProxy = (ReplicaStorage) replicaStorageServiceProxy.getProxy();
        ServiceProxy serviceProxy = (ServiceProxy) replicaStorageProxy;
        serviceProxy.getInvocationMetaData().setReplyAssessor(DoNotReplyWithNull.ASSESSOR);

        keyToReplicaInfo = new HashMap();
    }

    public void start() throws Exception {
        startStorageMonitoring();
    }

    public void stop() throws Exception {
        synchronized (keyToReplicaInfo) {
            // TODO - clear the storages
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

        CreateReplicaTask backOffCapableTask = new CreateReplicaTask(tmp, key);
        backOffCapableTask.attempt();
    }

    public void update(Object key, Object tmp) {
        ReplicaInfo replicaInfo;
        synchronized (keyToReplicaInfo) {
            replicaInfo = (ReplicaInfo) keyToReplicaInfo.get(key);
            if (null == replicaInfo) {
                throw new ReplicationKeyNotFoundException(key);
            }
            replicaInfo = new ReplicaInfo(replicaInfo, tmp);
            keyToReplicaInfo.put(key, replicaInfo);
        }

        if (replicaInfo.getSecondaries().length != 0) {
            cascadeUpdate(key, replicaInfo);
        }
    }

    public void destroy(Object key) {
        ReplicaInfo replicaInfo;
        synchronized (keyToReplicaInfo) {
            replicaInfo = (ReplicaInfo) keyToReplicaInfo.remove(key);
        }
        if (null == replicaInfo) {
            log.warn("Key [" + key + "] is not defined; cannot destroy it.");
            return;
        }
        
        if (replicaInfo.getSecondaries().length != 0) {
            cascadeDestroy(key, replicaInfo);
        }
    }

    public Object acquirePrimary(Object key) {
        ReplicaInfo replicaInfo;
        try {
            replicationManagerProxy.releasePrimary(key);
            replicaInfo = replicaStorageProxy.retrieveReplicaInfo(key);
        } catch (ServiceInvocationException e) {
            if (e.isMessageExchangeException()) {
                throw new InternalReplicationManagerException(e);
            } else {
                throw new ReplicationKeyNotFoundException(key, e);
            }
        }
        
        replicaInfo = reOrganizeSecondaries(key, replicaInfo);
        return replicaInfo.getReplica();
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
    
    public ReplicaInfo retrieveReplicaInfo(Object key) {
        ReplicaInfo replicaInfo;
        synchronized (keyToReplicaInfo) {
            replicaInfo = (ReplicaInfo) keyToReplicaInfo.get(key);
        }
        if (null == replicaInfo) {
            throw new ReplicationKeyNotFoundException(key);
        }
        return replicaInfo;
    }
    
    public boolean managePrimary(Object key) {
        synchronized (keyToReplicaInfo) {
            return keyToReplicaInfo.containsKey(key);
        }
    }

    protected void cascadeCreate(Object key, ReplicaInfo replicaInfo, BackOffCapableTask task) {
        ReplicaStorage storage = newReplicaStorageStub(replicaInfo.getSecondaries());
        try {
            storage.mergeCreate(key, replicaInfo);
            task.complete();
        } catch (ServiceInvocationException e) {
            if (e.isMessageExchangeException()) {
                task.backoff();
            } else {
                throw e;
            }
        }
    }
    
    protected void cascadeUpdate(Object key, ReplicaInfo replicaInfo) {
        ReplicaStorage storage = newReplicaStorageStub(replicaInfo.getSecondaries());
        try {
            storage.mergeUpdate(key, replicaInfo);
        } catch (ServiceInvocationException e) {
            if (e.isMessageExchangeException()) {
                log.warn("Update has not been properly cascaded due to a communication failure. If a targeted node " +
                        "has been lost, state will be re-balance automatically.", e);
            } else {
                throw new InternalReplicationManagerException(e);
            }
        }
    }
    
    protected void cascadeDestroy(Object key, ReplicaInfo replicaInfo) {
        ReplicaStorage storage = newReplicaStorageStub(replicaInfo.getSecondaries());
        ServiceProxy serviceProxy = (ServiceProxy) storage;
        serviceProxy.getInvocationMetaData().setOneWay(true);
        serviceProxy.getInvocationMetaData().setIgnoreMessageExchangeExceptionOnSend(true);
        storage.mergeDestroy(key);
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
    
    protected ReplicationManager newReplicationManagerProxy(ServiceSpace serviceSpace) {
        ServiceProxyFactory repManagerProxyFactory = serviceSpace.getServiceProxyFactory(ReplicationManager.NAME, 
                new Class[] {ReplicationManager.class});
        return (ReplicationManager) repManagerProxyFactory.getProxy();
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
        private volatile int currentAttempt;
        private volatile ReplicaInfo replicaInfo;

        private CreateReplicaTask(Object tmp, Object key) {
            this.key = key;
            this.tmp = tmp;
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
                replicaInfo = new ReplicaInfo(replicaInfo);
            }
            if (secondaries.length != 0) {
                cascadeCreate(key, replicaInfo, this);
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
