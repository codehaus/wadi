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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMLocalPeer;
import org.codehaus.wadi.group.vm.VMPeer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.common.ReplicaStorageInfo;
import org.codehaus.wadi.replication.manager.ReplicationKeyAlreadyExistsException;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.strategy.BackingStrategy;
import org.codehaus.wadi.servicespace.ServiceInvocationException;
import org.codehaus.wadi.servicespace.ServiceMonitor;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

public class SyncReplicationManagerTest extends RMockTestCase {
 
    private LocalPeer localPeer;
    private Peer peer2;
    private Peer peer3;
    private ServiceSpace serviceSpace;
    private ServiceMonitor storageMonitor;
    private BackingStrategy backingStrategy;
    private ObjectStateHandler stateHandler;
    private ProxyFactory proxyFactory;
    private ServiceProxyFactory replicaStorageServiceProxyFactory;
    private ReplicaStorage replicaStorageProxy;
    private ReplicaStorage localReplicaStorage;
    private HashMap<Object, ReplicaInfo> keyToReplicaInfo;
    private SecondaryManager secondaryManager;
    private Motable instance;
    private Object key;

    protected void setUp() throws Exception {
        keyToReplicaInfo = new HashMap<Object, ReplicaInfo>();
        
        localPeer = new VMLocalPeer("peer1");
        peer2 = new VMPeer("peer2", null);
        peer3 = new VMPeer("peer3", null);

        proxyFactory = (ProxyFactory) mock(ProxyFactory.class);
        replicaStorageServiceProxyFactory = proxyFactory.newReplicaStorageServiceProxyFactory();
        replicaStorageProxy = proxyFactory.newReplicaStorageProxy();

        serviceSpace = (ServiceSpace) mock(ServiceSpace.class);
        serviceSpace.getLocalPeer();
        modify().returnValue(localPeer);
        
        storageMonitor = serviceSpace.getServiceMonitor(ReplicaStorage.NAME);
        storageMonitor.addServiceLifecycleListener(null);
        modify().args(is.NOT_NULL);
        
        backingStrategy = (BackingStrategy) mock(BackingStrategy.class);
        stateHandler = (ObjectStateHandler) mock(ObjectStateHandler.class);
        localReplicaStorage = (ReplicaStorage) mock(ReplicaStorage.class);
        secondaryManager = (SecondaryManager) mock(SecondaryManager.class);
        
        key = new Object();
        instance = (Motable) mock(Motable.class);
    }

    public void testStart() throws Exception {
        beginSection(s.ordered("Start"));
        storageMonitor.start();
        storageMonitor.getHostingPeers();
        modify().returnValue(Collections.singleton(peer2));
        
        backingStrategy.addSecondaries(new Peer[] {peer2});

        modify().args(is.NOT_NULL);
        endSection();
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        manager.start();
    }
    
    public void testRetrieveReplicaWithNoReplicaReturnsNull() throws Exception {
        Object key = new Object();
        replicaStorageProxy.retrieveReplicaStorageInfo(key);
        modify().throwException(new ServiceInvocationException(new MessageExchangeException("desc")));
        
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        Object retrieveReplica = manager.retrieveReplica(key);
        assertNull(retrieveReplica);
    }

    public void testRetrieveReplicaWithFoundReplica() throws Exception {
        ReplicaInfo replicaInfo = new ReplicaInfo(peer2, new Peer[] {peer3}, instance);
        ReplicaStorageInfo replicaStorageInfo = new ReplicaStorageInfo(replicaInfo, new byte[0]);
        
        replicaStorageProxy.retrieveReplicaStorageInfo(key);
        modify().returnValue(replicaStorageInfo);
        stateHandler.restoreFromFullStateTransient(key, replicaStorageInfo.getSerializedPayload());
        modify().returnValue(instance);
        stateHandler.resetObjectState(instance);
        
        secondaryManager.updateSecondariesFollowingRestoreFromSecondary(key, replicaInfo);
        modify().returnValue(replicaInfo);
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        Object retrieveReplica = manager.retrieveReplica(key);
        assertSame(instance, retrieveReplica);
    }

    public void testReleaseReplicaInfoWhenLocalPeerBecomesSecondary() throws Exception {
        Peer[] secondaries = new Peer[] {peer3};
        
        keyToReplicaInfo.put(key, new ReplicaInfo(localPeer, secondaries, instance));
        
        backingStrategy.reElectSecondariesForSwap(key, peer3, secondaries);
        final Peer[] newSecondaries = new Peer[] {localPeer};
        modify().returnValue(newSecondaries);
        
        localReplicaStorage.insert(key, null);
        modify().args(is.AS_RECORDED, new AbstractExpression() {
            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                ReplicaInfo replicaInfo = (ReplicaInfo) arg0;
                assertSame(newSecondaries, replicaInfo.getSecondaries());
                return true;
            }
        });
        
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        manager.releaseReplicaInfo(key, peer3);
    }
    
    public void testReleaseReplicaInfoWhenLocalPeerDoesNotBecomesSecondary() throws Exception {
        Peer[] secondaries = new Peer[] {peer2};
        
        keyToReplicaInfo.put(key, new ReplicaInfo(localPeer, secondaries, instance));
        
        backingStrategy.reElectSecondariesForSwap(key, peer3, secondaries);
        modify().returnValue(secondaries);
        
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        manager.releaseReplicaInfo(key, peer3);
    }
    
    public void testReleaseUnknownReplicaInfoRetunsNull() throws Exception {
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        ReplicaInfo replicaInfo = manager.releaseReplicaInfo("key", localPeer);
        assertNull(replicaInfo);
    }
    
    public void testInsertReplicaInfo() throws Exception {
        ReplicaInfo replicaInfo = new ReplicaInfo(localPeer, new Peer[0], instance);
        
        localReplicaStorage.mergeDestroyIfExist(key);
        
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        manager.insertReplicaInfo(key, replicaInfo);
        
        assertTrue(keyToReplicaInfo.containsKey(key));
    }
    
    public void testInsertReplicaInfoForExistingKeyFails() throws Exception {
        ReplicaInfo replicaInfo = new ReplicaInfo(localPeer, new Peer[0], instance);
        keyToReplicaInfo.put(key, replicaInfo);
        
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        try {
            manager.insertReplicaInfo(key, replicaInfo);
            fail();
        } catch (ReplicationKeyAlreadyExistsException e) {
        }
    }
    
    public void testPromoteToMasterWithReplicaInfoSetPayloadAndAddReplicaInfo() throws Exception {
        Motable motable = (Motable) mock(Motable.class);
        localReplicaStorage.mergeDestroyIfExist(key);
        
        startVerification();

        ReplicaInfo replicaInfo = new ReplicaInfo(localPeer, new Peer[0], instance);
        
        SyncReplicationManager manager = newReplicationManager();
        manager.promoteToMaster(key, replicaInfo, motable, null);
        
        assertSame(motable, replicaInfo.getPayload());
        assertTrue(keyToReplicaInfo.containsKey(key));
    }
    
    protected SyncReplicationManager newReplicationManager() {
        return new SyncReplicationManager(serviceSpace,
            stateHandler,
            backingStrategy,
            localReplicaStorage,
            proxyFactory) {
            @Override
            protected Map<Object, ReplicaInfo> newKeyToReplicaInfo() {
                return keyToReplicaInfo;
            }
            @Override
            protected SecondaryManager newSecondaryManager() {
                return secondaryManager;
            }
        };
    }
}
