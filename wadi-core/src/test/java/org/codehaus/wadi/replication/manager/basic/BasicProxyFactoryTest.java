/**
 * Copyright 2007 The Apache Software Foundation
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

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.servicespace.InvocationMetaData;
import org.codehaus.wadi.servicespace.ServiceProxy;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.replyaccessor.DoNotReplyWithNull;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 * 
 * @version $Revision: 2340 $
 */
public class BasicProxyFactoryTest extends RMockTestCase {
    private ServiceSpace serviceSpace;
    private BasicProxyFactory proxyFactory;

    @Override
    protected void setUp() throws Exception {
        serviceSpace = (ServiceSpace) mock(ServiceSpace.class);
        proxyFactory = new BasicProxyFactory(serviceSpace);
    }
    
    public void testNewReplicaStorageServiceProxyFactory() throws Exception {
        serviceSpace.getServiceProxyFactory(ReplicaStorage.NAME, new Class[] {ReplicaStorage.class});
        startVerification();
        
        proxyFactory.newReplicaStorageServiceProxyFactory();
    }
    
    public void testNewReplicaStorageProxy() throws Exception {
        ServiceProxyFactory serviceProxyFactory = 
            serviceSpace.getServiceProxyFactory(ReplicaStorage.NAME, new Class[] {ReplicaStorage.class});
        
        serviceProxyFactory.getProxy();
        ReplicaStorage replicaStorage = (ReplicaStorage) mock(ReplicaStorageMixInServiceProxy.class);
        modify().returnValue(replicaStorage);
        
        InvocationMetaData invMetaData = 
            (InvocationMetaData) intercept(InvocationMetaData.class, "replicaStorageProxyInvMetaData");
        ((ServiceProxy) replicaStorage).getInvocationMetaData();
        modify().returnValue(invMetaData);
        invMetaData.setReplyAssessor(DoNotReplyWithNull.ASSESSOR);

        startVerification();
        
        proxyFactory.newReplicaStorageProxy();
    }
    
    public void testNewReplicaStorageProxyWithPeers() throws Exception {
        Peer[] targets = new Peer[0];
        
        ServiceProxyFactory serviceProxyFactory = 
            serviceSpace.getServiceProxyFactory(ReplicaStorage.NAME, new Class[] {ReplicaStorage.class});
        
        serviceProxyFactory.getProxy();
        ReplicaStorage replicaStorage = (ReplicaStorage) mock(ReplicaStorageMixInServiceProxy.class);
        modify().returnValue(replicaStorage);
        
        InvocationMetaData invMetaData = 
            (InvocationMetaData) intercept(InvocationMetaData.class, "replicaStorageProxyInvMetaData");
        ((ServiceProxy) replicaStorage).getInvocationMetaData();
        modify().returnValue(invMetaData);
        invMetaData.setTargets(targets);

        startVerification();
        
        proxyFactory.newReplicaStorageProxy(targets);
    }
    
    public interface ReplicaStorageMixInServiceProxy extends ReplicaStorage, ServiceProxy {
    }
    
    public interface ReplicationManagerMixInServiceProxy extends ReplicationManager, ServiceProxy {
    }

}
