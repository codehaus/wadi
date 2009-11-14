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

import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.manager.ReplicationManagerFactory;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.strategy.BackingStrategy;
import org.codehaus.wadi.replication.strategy.BackingStrategyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;

/**
 * 
 * @version $Revision$
 */
public class SyncReplicationManagerFactory implements ReplicationManagerFactory {

    private final ObjectStateHandler stateHandler;
    private final ReplicaStorage localReplicaStorage;
    
    public SyncReplicationManagerFactory(ObjectStateHandler stateHandler, ReplicaStorage localReplicaStorage) {
        if (null == stateHandler) {
            throw new IllegalArgumentException("stateHandler is required");
        } else if (null == localReplicaStorage) {
            throw new IllegalArgumentException("localReplicaStorage is required");
        }
        this.stateHandler = stateHandler;
        this.localReplicaStorage = localReplicaStorage;
    }

    public ReplicationManager factory(ServiceSpace serviceSpace, BackingStrategyFactory backingStrategyFactory) {
        backingStrategyFactory.setServiceSpace(serviceSpace);
        BackingStrategy backingStrategy = backingStrategyFactory.factory();
        return new SyncReplicationManager(serviceSpace, stateHandler, backingStrategy, localReplicaStorage);
    }

}
