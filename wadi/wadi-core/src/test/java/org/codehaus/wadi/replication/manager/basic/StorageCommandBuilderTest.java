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

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMPeer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.servicespace.InvocationMetaData;
import org.codehaus.wadi.servicespace.ServiceProxy;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;

import com.agical.rmock.extension.junit.RMockTestCase;

public class StorageCommandBuilderTest extends RMockTestCase {

    private Peer peer1;
    private Peer peer2;
    private Peer peer3;
    private Peer peer4;

    protected void setUp() throws Exception {
        peer1 = new VMPeer("peer1");
        peer2 = new VMPeer("peer2");
        peer3 = new VMPeer("peer3");
        peer4 = new VMPeer("peer4");
    }
    
    public void testBuild() throws Exception {
        InvocationMetaData invMetaData = (InvocationMetaData) intercept(InvocationMetaData.class, "invMetaData");
        
        String expectedKey = "key";
        Object payload = new Object();
        ReplicaInfo expectedReplicaInfo = new ReplicaInfo(peer1, new Peer[] {peer2, peer3}, payload);

        ReplicaStorageMixInServiceProxy storage = (ReplicaStorageMixInServiceProxy) mock(ReplicaStorageMixInServiceProxy.class);

        ServiceProxyFactory serviceProxy = (ServiceProxyFactory) mock(ServiceProxyFactory.class);
        beginSection(s.ordered("create - update - destroy"));
        serviceProxy.getProxy();
        modify().returnValue(storage);
        storage.getInvocationMetaData();
        modify().returnValue(invMetaData);
        invMetaData.setTargets(new Peer[] {peer2});
        storage.mergeCreate(expectedKey, expectedReplicaInfo);
        modify().args(is.AS_RECORDED, is.NOT_NULL);

        serviceProxy.getProxy();
        modify().returnValue(storage);
        storage.getInvocationMetaData();
        modify().returnValue(invMetaData);
        invMetaData.setTargets(new Peer[] {peer3});
        storage.mergeUpdate(expectedKey, new ReplicaInfo(peer1, expectedReplicaInfo.getSecondaries(), payload));
        modify().args(is.AS_RECORDED, is.NOT_NULL);

        serviceProxy.getProxy();
        modify().returnValue(storage);
        storage.getInvocationMetaData();
        modify().returnValue(invMetaData);
        invMetaData.setTargets(new Peer[] {peer4});
        storage.mergeDestroy(expectedKey);
        endSection();

        startVerification();
        
        StorageCommandBuilder builder = new StorageCommandBuilder(expectedKey, expectedReplicaInfo, 
                new Peer[] {peer3, peer4});
        
        StorageCommand[] commands = builder.build();
        assertEquals(3, commands.length);

        commands[0].execute(serviceProxy);
        commands[1].execute(serviceProxy);
        commands[2].execute(serviceProxy);
    }
    
    public interface ReplicaStorageMixInServiceProxy extends ReplicaStorage, ServiceProxy {
    }
}
