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
import org.codehaus.wadi.servicespace.ServiceProxy;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.replyaccessor.DoNotReplyWithNull;

/**
 * 
 * @version $Revision: 2340 $
 */
public class BasicProxyFactory implements ProxyFactory {
    
    private final ServiceSpace serviceSpace;
    private ServiceProxyFactory replicaStorageServiceProxy;

    public BasicProxyFactory(ServiceSpace serviceSpace) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        }
        this.serviceSpace = serviceSpace;
    }

    public ReplicationManager newReplicationManagerProxy() {
        ServiceProxyFactory repManagerProxyFactory = serviceSpace.getServiceProxyFactory(ReplicationManager.NAME, 
            new Class[] {ReplicationManager.class});
        return (ReplicationManager) repManagerProxyFactory.getProxy();
    }

    public ServiceProxyFactory newReplicaStorageServiceProxyFactory() {
        if (null == replicaStorageServiceProxy) {
            replicaStorageServiceProxy = serviceSpace.getServiceProxyFactory(
                ReplicaStorage.NAME, new Class[] {ReplicaStorage.class});
        }
        return replicaStorageServiceProxy;
    }
    
    public ReplicaStorage newReplicaStorageProxy() {
        ServiceProxy serviceProxy = newReplicaStorageServiceProxyFactory().getProxy();
        serviceProxy.getInvocationMetaData().setReplyAssessor(DoNotReplyWithNull.ASSESSOR);
        return (ReplicaStorage) serviceProxy;
    }

    public ReplicaStorage newReplicaStorageProxy(Peer[] peers) {
        ServiceProxy serviceProxy = newReplicaStorageServiceProxyFactory().getProxy();
        serviceProxy.getInvocationMetaData().setTargets(peers);
        return (ReplicaStorage) serviceProxy;
    }

    public ReplicaStorage newReplicaStorageProxyForDelete(Peer[] peers) {
        ServiceProxy serviceProxy = newReplicaStorageServiceProxyFactory().getProxy();
        serviceProxy.getInvocationMetaData().setTargets(peers);
        serviceProxy.getInvocationMetaData().setOneWay(true);
        serviceProxy.getInvocationMetaData().setIgnoreMessageExchangeExceptionOnSend(true);
        return (ReplicaStorage) serviceProxy;
    }

}