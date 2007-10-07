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
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMLocalPeer;
import org.codehaus.wadi.group.vm.VMPeer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.common.ReplicaStorageInfo;
import org.codehaus.wadi.replication.manager.ReplicationKeyAlreadyExistsException;
import org.codehaus.wadi.replication.manager.ReplicationKeyNotFoundException;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.strategy.BackingStrategy;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceInvocationException;
import org.codehaus.wadi.servicespace.ServiceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceListener;
import org.codehaus.wadi.servicespace.ServiceMonitor;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

import com.agical.rmock.core.Action;
import com.agical.rmock.core.MethodHandle;
import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

public class SyncReplicationManagerTest extends RMockTestCase {
 
    private LocalPeer localPeer;
    private Peer peer2;
    private Peer peer3;
    private Peer peer4;
    private ServiceSpace serviceSpace;
    private ServiceMonitor storageMonitor;
    private ServiceListener serviceListener;
    private BackingStrategy backingStrategy;
    private ServiceSpaceName serviceSpaceName;
    private ObjectStateHandler stateHandler;
    private ProxyFactory proxyFactory;
    private ServiceProxyFactory replicaStorageServiceProxyFactory;
    private ReplicaStorage replicaStorageProxy;
    private ReplicaStorage localReplicaStorage;
    private HashMap<Object, ReplicaInfo> keyToReplicaInfo;

    protected void setUp() throws Exception {
        keyToReplicaInfo = new HashMap<Object, ReplicaInfo>();
        
        localPeer = new VMLocalPeer("peer1");
        peer2 = new VMPeer("peer2", null);
        peer3 = new VMPeer("peer3", null);
        peer4 = new VMPeer("peer4", null);

        proxyFactory = (ProxyFactory) mock(ProxyFactory.class);
        replicaStorageServiceProxyFactory = proxyFactory.newReplicaStorageServiceProxyFactory();
        replicaStorageProxy = proxyFactory.newReplicaStorageProxy();

        serviceSpace = (ServiceSpace) mock(ServiceSpace.class);
        serviceSpace.getLocalPeer();
        modify().returnValue(localPeer);
        
        serviceSpaceName = new ServiceSpaceName(new URI("name"));
        storageMonitor = serviceSpace.getServiceMonitor(ReplicaStorage.NAME);
        storageMonitor.addServiceLifecycleListener(null);
        modify().args(is.NOT_NULL);
        modify().perform(new Action() {

            public Object invocation(Object[] arguments, MethodHandle methodHandle) throws Throwable {
                serviceListener = (ServiceListener) arguments[0];
                return null;
            }
            
        });
        
        backingStrategy = (BackingStrategy) mock(BackingStrategy.class);
        stateHandler = (ObjectStateHandler) mock(ObjectStateHandler.class);
        localReplicaStorage = (ReplicaStorage) mock(ReplicaStorage.class);
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
    
    public void testStorageListener() throws Exception {
        beginSection(s.ordered("ordered secondary un/registration"));
        backingStrategy.addSecondary(peer3);
        backingStrategy.addSecondary(peer4);
        backingStrategy.removeSecondary(peer3);
        backingStrategy.removeSecondary(peer4);
        endSection();
        startVerification();
        
        newReplicationManager();
        
        receiveEvent(peer3, LifecycleState.AVAILABLE);
        receiveEvent(peer4, LifecycleState.STARTED);
        receiveEvent(peer3, LifecycleState.STOPPING);
        receiveEvent(peer4, LifecycleState.FAILED);
    }
    
    public void testCreate() throws Exception {
        Object key = new Object();
        Object instance = new Object();
        Peer[] targets = new Peer[] {peer2};
        
        recordCreate(key, instance, targets);

        ReplicaStorage newReplicaStorageProxy = proxyFactory.newReplicaStorageProxy(targets);
        newReplicaStorageProxy.mergeCreate(key, null);
        modify().args(is.AS_RECORDED, is.ANYTHING);
        
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        manager.create(key, instance);
        assertTrue(manager.getManagedReplicaInfoKeys().contains(key));
    }

    public void testUpdate() throws Exception {
        Object key = new Object();
        Object instance = new Object();
        Peer[] targets = new Peer[] {peer2};
        Object newInstance = new Object();

        keyToReplicaInfo.put(key, new ReplicaInfo(localPeer, targets, instance));
        
        beginSection(s.ordered("Extract Uupdate; Reset state; Elect Secondaries"));
        stateHandler.extractUpdatedState(key, newInstance);
        modify().returnValue(new byte[0]);
        
        stateHandler.resetObjectState(newInstance);
        
        ReplicaStorage newReplicaStorageProxy = proxyFactory.newReplicaStorageProxy(targets);
        newReplicaStorageProxy.mergeUpdate(key, null);
        modify().args(is.AS_RECORDED, is.ANYTHING);
        
        endSection();
        
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        manager.update(key, newInstance);
        assertTrue(manager.getManagedReplicaInfoKeys().contains(key));
    }
    
    public void testDelete() throws Exception {
        Object key = new Object();
        Object instance = new Object();
        
        keyToReplicaInfo.put(key, new ReplicaInfo(localPeer, new Peer[0], instance));

        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        manager.destroy(key);
        assertFalse(manager.getManagedReplicaInfoKeys().contains(key));
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
        Object key = new Object();
        Object instance = new Object();
        ReplicaInfo replicaInfo = new ReplicaInfo(peer2, new Peer[] {peer3}, instance);
        ReplicaStorageInfo replicaStorageInfo = new ReplicaStorageInfo(replicaInfo, new byte[0]);
        
        replicaStorageProxy.retrieveReplicaStorageInfo(key);
        modify().returnValue(replicaStorageInfo);
        stateHandler.restoreFromFullStateTransient(key, replicaStorageInfo.getSerializedPayload());
        modify().returnValue(instance);
        stateHandler.resetObjectState(instance);
        
        backingStrategy.reElectSecondaries(key, replicaInfo.getPrimary(), replicaInfo.getSecondaries());
        modify().returnValue(new Peer[] {peer4});
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        Object retrieveReplica = manager.retrieveReplica(key);
        assertSame(instance, retrieveReplica);
    }

    public void testReleaseReplicaInfoWhenLocalPeerBecomesSecondary() throws Exception {
        Object key = new Object();
        Object instance = new Object();
        Peer[] secondaries = new Peer[] {peer3};
        
        keyToReplicaInfo.put(key, new ReplicaInfo(localPeer, secondaries, instance));
        
        backingStrategy.reElectSecondariesForSwap(key, peer3, secondaries);
        final Peer[] newSecondaries = new Peer[] {localPeer};
        modify().returnValue(newSecondaries);
        
        stateHandler.extractFullState(key, instance);
        final byte[] fullState = new byte[0];
        modify().returnValue(fullState);
        
        localReplicaStorage.insert(key, null);
        modify().args(is.AS_RECORDED, new AbstractExpression() {
            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                ReplicaStorageInfo storageInfo = (ReplicaStorageInfo) arg0;
                assertSame(fullState, storageInfo.getSerializedPayload());
                assertSame(newSecondaries, storageInfo.getReplicaInfo().getSecondaries());
                return true;
            }
        });
        
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        manager.releaseReplicaInfo(key, peer3);
    }
    
    public void testReleaseReplicaInfoWhenLocalPeerDoesNotBecomesSecondary() throws Exception {
        Object key = new Object();
        Object instance = new Object();
        Peer[] secondaries = new Peer[] {peer2};
        
        keyToReplicaInfo.put(key, new ReplicaInfo(localPeer, secondaries, instance));
        
        backingStrategy.reElectSecondariesForSwap(key, peer3, secondaries);
        modify().returnValue(secondaries);
        
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        manager.releaseReplicaInfo(key, peer3);
    }
    
    public void testReleaseUnknownReplicaInfoFails() throws Exception {
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        try {
            manager.releaseReplicaInfo("key", localPeer);
            fail();
        } catch (ReplicationKeyNotFoundException e) {
        }
    }
    
    public void testInsertReplicaInfo() throws Exception {
        Object key = new Object();
        ReplicaInfo replicaInfo = new ReplicaInfo(localPeer, new Peer[0], new Object());
        
        localReplicaStorage.mergeDestroyIfExist(key);
        
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        manager.insertReplicaInfo(key, replicaInfo);
        
        assertTrue(keyToReplicaInfo.containsKey(key));
    }
    
    public void testInsertReplicaInfoForExistingKeyFails() throws Exception {
        Object key = new Object();
        ReplicaInfo replicaInfo = new ReplicaInfo(localPeer, new Peer[0], new Object());
        keyToReplicaInfo.put(key, replicaInfo);
        
        startVerification();
        
        SyncReplicationManager manager = newReplicationManager();
        try {
            manager.insertReplicaInfo(key, replicaInfo);
            fail();
        } catch (ReplicationKeyAlreadyExistsException e) {
        }
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
            protected void updateReplicaStorages(Object key, ReplicaInfo replicaInfo, Peer[] oldSecondaries) {
            }
        };
    }
    
    protected void recordCreate(Object key, Object instance, Peer[] targets) {
        beginSection(s.ordered("Extract State; Reset state; Elect Secondaries; mergeCreate"));
        stateHandler.extractFullState(key, instance);
        modify().returnValue(new byte[0]);
        
        stateHandler.resetObjectState(instance);
        
        backingStrategy.electSecondaries(key);
        modify().returnValue(targets);
        endSection();
    }

    private void receiveEvent(Peer peer, LifecycleState state) {
        serviceListener.receive(new ServiceLifecycleEvent(serviceSpaceName, ReplicaStorage.NAME, peer, state), 
                Collections.EMPTY_SET);
    }
    
}
