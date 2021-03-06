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
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.servicespace.InvocationMetaData;
import org.codehaus.wadi.servicespace.ServiceProxy;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;


class DestroyStorageCommand extends AbstractStorageCommand {
    
    public DestroyStorageCommand(Peer[] targets, Object key) {
        super(targets, key, null);
    }
    
    public void execute(ServiceProxyFactory serviceProxy) {
        ReplicaStorage storage = (ReplicaStorage) serviceProxy.getProxy();
        InvocationMetaData invocationMetaData = ((ServiceProxy) storage).getInvocationMetaData();
        invocationMetaData.setTargets(targets);
        invocationMetaData.setOneWay(true);
        storage.mergeDestroy(key);
    }
    
}