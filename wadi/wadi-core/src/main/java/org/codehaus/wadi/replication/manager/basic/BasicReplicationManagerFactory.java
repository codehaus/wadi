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

import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.replication.common.NodeInfo;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.manager.ReplicationManagerFactory;
import org.codehaus.wadi.replication.manager.remoting.BasicReplicationManagerExporter;
import org.codehaus.wadi.replication.manager.remoting.BasicReplicationManagerStubFactory;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.storage.ReplicaStorageFactory;
import org.codehaus.wadi.replication.storage.ReplicaStorageStubFactory;
import org.codehaus.wadi.replication.storage.remoting.BasicReplicaStorageMonitor;
import org.codehaus.wadi.replication.storage.remoting.BasicReplicaStorageStubFactory;
import org.codehaus.wadi.replication.strategy.BackingStrategy;
import org.codehaus.wadi.replication.strategy.BackingStrategyFactory;

/**
 * 
 * @version $Revision$
 */
public class BasicReplicationManagerFactory implements ReplicationManagerFactory {

    public ReplicationManager factory(Dispatcher dispatcher, 
            ReplicaStorageFactory replicaStoragefactory,
            BackingStrategyFactory backingStrategyFactory) {
        NodeInfo nodeInfo = new NodeInfo(dispatcher.getPeerName());
        
        BackingStrategy backingStrategy = backingStrategyFactory.factory();
        ReplicaStorage storage = replicaStoragefactory.factory(dispatcher);
        
        return new BasicReplicationManager(
                backingStrategy,
                new BasicReplicationManagerStubFactory(dispatcher),
                newReplicaStorageStubFactory(dispatcher),
                new BasicReplicaStorageMonitor(dispatcher),
                storage,
                new BasicReplicationManagerExporter(dispatcher), 
                nodeInfo);
    }

    protected ReplicaStorageStubFactory newReplicaStorageStubFactory(Dispatcher dispatcher) {
        return new BasicReplicaStorageStubFactory(dispatcher);
    }

}
