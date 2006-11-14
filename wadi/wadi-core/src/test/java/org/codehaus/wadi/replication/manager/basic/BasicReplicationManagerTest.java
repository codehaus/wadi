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

import java.net.URI;
import java.util.Collections;

import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMLocalPeer;
import org.codehaus.wadi.group.vm.VMPeer;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.strategy.BackingStrategy;
import org.codehaus.wadi.servicespace.InvocationMetaData;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceListener;
import org.codehaus.wadi.servicespace.ServiceMonitor;
import org.codehaus.wadi.servicespace.ServiceProxy;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.replyaccessor.DoNotReplyWithNull;

import com.agical.rmock.core.Action;
import com.agical.rmock.core.MethodHandle;
import com.agical.rmock.extension.junit.RMockTestCase;

public class BasicReplicationManagerTest extends RMockTestCase {
 
    private LocalPeer localPeer;
    private Peer peer2;
    private Peer peer3;
    private Peer peer4;
    private ServiceSpace serviceSpace;
    private ServiceMonitor storageMonitor;
    private ServiceListener serviceListener;
    private BackingStrategy backingStrategy;
    private ServiceSpaceName serviceSpaceName;
    private ServiceProxyFactory replicaStorageServiceProxyFactory;
    private ReplicaStorageMixInServiceProxy replicaStorageProxy;
    private ReplicationManager replicationManagerProxy;

    protected void setUp() throws Exception {
        localPeer = new VMLocalPeer("peer1");
        peer2 = new VMPeer("peer2", null);
        peer3 = new VMPeer("peer3", null);
        peer4 = new VMPeer("peer4", null);
        
        serviceSpace = (ServiceSpace) mock(ServiceSpace.class);
        serviceSpace.getLocalPeer();
        modify().returnValue(localPeer);
        
        recordCreateProxies();
        
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
        
        BasicReplicationManager manager = new BasicReplicationManager(serviceSpace, backingStrategy);
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
        
        new BasicReplicationManager(serviceSpace, backingStrategy);
        
        receiveEvent(peer3, LifecycleState.AVAILABLE);
        receiveEvent(peer4, LifecycleState.STARTED);
        receiveEvent(peer3, LifecycleState.STOPPING);
        receiveEvent(peer4, LifecycleState.FAILED);
    }
    
    private void receiveEvent(Peer peer, LifecycleState state) {
        serviceListener.receive(new ServiceLifecycleEvent(serviceSpaceName, ReplicaStorage.NAME, peer, state), 
                Collections.EMPTY_SET);
    }
    
    private void recordCreateProxies() {
        recordCreateReplicationManagerProxy();
        recordCreateReplicaStorageProxy();
    }

    private void recordCreateReplicationManagerProxy() {
        beginSection(s.ordered("Create ReplicationManager proxy"));
        InvocationMetaData invMetaData = (InvocationMetaData) intercept(InvocationMetaData.class, "invMetaData");
        ServiceProxyFactory serviceProxyFactory =
            serviceSpace.getServiceProxyFactory(ReplicationManager.NAME, new Class[] {ReplicationManager.class});
        serviceProxyFactory.getInvocationMetaData();
        modify().returnValue(invMetaData);
        invMetaData.setOneWay(true);
        modify().forward();
        serviceProxyFactory.getProxy();
        replicationManagerProxy = (ReplicationManager) mock(ReplicationManager.class);
        modify().returnValue(replicationManagerProxy);
        endSection();
    }
    
    private void recordCreateReplicaStorageProxy() {
        beginSection(s.ordered("Create ReplicaStorage proxy"));
        replicaStorageServiceProxyFactory = serviceSpace.getServiceProxyFactory(ReplicaStorage.NAME, new Class[] {ReplicaStorage.class});
        replicaStorageServiceProxyFactory.getProxy();
        replicaStorageProxy = (ReplicaStorageMixInServiceProxy) mock(ReplicaStorageMixInServiceProxy.class);
        modify().returnValue(replicaStorageProxy);
        
        InvocationMetaData invMetaData = 
            (InvocationMetaData) intercept(InvocationMetaData.class, "replicaStorageProxyInvMetaData");
        replicaStorageProxy.getInvocationMetaData();
        modify().returnValue(invMetaData);
        invMetaData.setReplyAssessor(DoNotReplyWithNull.ASSESSOR);
        modify().forward();

        endSection();
    }

    public interface ReplicaStorageMixInServiceProxy extends ReplicaStorage, ServiceProxy {
    }
    
}
