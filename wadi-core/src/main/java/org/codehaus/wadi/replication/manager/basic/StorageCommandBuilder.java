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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.replication.common.ReplicaInfo;

class StorageCommandBuilder {
    private final Object key;
    private final ReplicaInfo replicaInfo;
    private final Peer[] oldSecondaries;
    private final ObjectStateHandler stateHandler;
    
    public StorageCommandBuilder(Object key,
            ReplicaInfo replicaInfo,
            Peer[] oldSecondaries,
            ObjectStateHandler stateHandler) {
        if (null == key) {
            throw new IllegalArgumentException("key is required");
        } else if (null == replicaInfo) {
            throw new IllegalArgumentException("replicaInfo is required");
        } else if (null == oldSecondaries) {
            throw new IllegalArgumentException("oldSecondaries is required");
        } else if (null == stateHandler) {
            throw new IllegalArgumentException("stateHandler is required");
        }
        this.key = key;
        this.replicaInfo = replicaInfo;
        this.oldSecondaries = oldSecondaries;
        this.stateHandler = stateHandler;
    }

    public StorageCommand[] build() {
        Set<Peer> destroySecondariesSet = new HashSet<Peer>();
        for (int i = 0; i < oldSecondaries.length; i++) {
            destroySecondariesSet.add(oldSecondaries[i]);
        }

        Peer newSecondaries[] = replicaInfo.getSecondaries();
        
        Set<Peer> createSecondariesSet = new HashSet<Peer>();
        Set<Peer> updateSecondariesSet = new HashSet<Peer>();
        for (int i = 0; i < newSecondaries.length; i++) {
            Peer secondary = newSecondaries[i];
            boolean exist = destroySecondariesSet.remove(secondary);
            if (exist) {
                updateSecondariesSet.add(secondary);
            } else {
                createSecondariesSet.add(secondary);
            }
        }
        
        Collection<StorageCommand> commands = new ArrayList<StorageCommand>(2);

        if (0 != createSecondariesSet.size()) {
            Peer targets[] = (Peer[]) createSecondariesSet.toArray(new Peer[0]);
            commands.add(new CreateStorageCommand(targets, key, replicaInfo, stateHandler));
        }

        if (0 != destroySecondariesSet.size()) {
            Peer targets[] = (Peer[]) destroySecondariesSet.toArray(new Peer[0]);
            commands.add(new DestroyStorageCommand(targets,key));
        }

        return commands.toArray(new StorageCommand[0]);
    }
}
