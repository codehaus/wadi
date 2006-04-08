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

import org.codehaus.wadi.replication.common.NodeInfo;
import org.codehaus.wadi.replication.common.ReplicaInfo;

class StorageCommandBuilder {
    private final Object key;
    private final ReplicaInfo replicaInfo;
    private final NodeInfo[] oldSecondaries;
    
    public StorageCommandBuilder(Object key, ReplicaInfo replicaInfo, NodeInfo[] oldSecondaries) {
        this.key = key;
        this.replicaInfo = replicaInfo;
        this.oldSecondaries = oldSecondaries;
    }

    public StorageCommand[] build() {
        Set destroySecondariesSet = new HashSet();
        for (int i = 0; i < oldSecondaries.length; i++) {
            destroySecondariesSet.add(oldSecondaries[i]);
        }

        NodeInfo newSecondaries[] = replicaInfo.getSecondaries();
        
        Set createSecondariesSet = new HashSet();
        Set updateSecondariesSet = new HashSet();
        for (int i = 0; i < newSecondaries.length; i++) {
            NodeInfo secondary = newSecondaries[i];
            boolean exist = destroySecondariesSet.remove(secondary);
            if (exist) {
                updateSecondariesSet.add(secondary);
            } else {
                createSecondariesSet.add(secondary);
            }
        }
        
        Collection commands = new ArrayList(3);

        if (0 != createSecondariesSet.size()) {
            NodeInfo targets[] = (NodeInfo[]) createSecondariesSet.toArray(new NodeInfo[0]);
            commands.add(
                    new CreateStorageCommand(targets,
                            key,
                            replicaInfo));
        }

        if (0 != updateSecondariesSet.size()) {
            NodeInfo targets[] = (NodeInfo[]) updateSecondariesSet.toArray(new NodeInfo[0]);
            commands.add(
                    new UpdateStorageCommand(targets,
                            key,
                            new ReplicaInfo(replicaInfo.getPrimary(), newSecondaries, null)));
        }

        if (0 != destroySecondariesSet.size()) {
            NodeInfo targets[] = (NodeInfo[]) destroySecondariesSet.toArray(new NodeInfo[0]);
            commands.add(new DestroyStorageCommand(targets,key));
        }

        return (StorageCommand[]) commands.toArray(new StorageCommand[0]);
    }
}
