/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codehaus.wadi.replication.manager.basic;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.strategy.BackingStrategy;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;

/**
 * 
 * @version $Revision: 2340 $
 */
public class SyncSecondaryManager implements SecondaryManager {
    private final Map<Object, ReplicaInfo> keyToReplicaInfo;
    private final BackingStrategy backingStrategy;
    private final LocalPeer localPeer;
    private final ObjectStateHandler stateHandler;
    private final ServiceProxyFactory replicaStorageServiceProxy;

    public SyncSecondaryManager(Map<Object, ReplicaInfo> keyToReplicaInfo,
            BackingStrategy backingStrategy,
            LocalPeer localPeer,
            ObjectStateHandler stateHandler,
            ServiceProxyFactory replicaStorageServiceProxy) {
        if (null == keyToReplicaInfo) {
            throw new IllegalArgumentException("keyToReplicaInfo is required");
        } else if (null == backingStrategy) {
            throw new IllegalArgumentException("backingStrategy is required");
        } else if (null == localPeer) {
            throw new IllegalArgumentException("localPeer is required");
        } else if (null == stateHandler) {
            throw new IllegalArgumentException("stateHandler is required");
        } else if (null == replicaStorageServiceProxy) {
            throw new IllegalArgumentException("replicaStorageServiceProxy is required");
        }
        this.keyToReplicaInfo = keyToReplicaInfo;
        this.backingStrategy = backingStrategy;
        this.localPeer = localPeer;
        this.stateHandler = stateHandler;
        this.replicaStorageServiceProxy = replicaStorageServiceProxy;
    }

    public void updateSecondariesFollowingJoiningPeer(Peer joiningPeer) {
        Map<Object, ReplicaInfo> tmpKeyToReplicaInfo;
        synchronized (keyToReplicaInfo) {
            tmpKeyToReplicaInfo = new HashMap<Object, ReplicaInfo>(keyToReplicaInfo);
        }
        for (Map.Entry<Object, ReplicaInfo> entry : tmpKeyToReplicaInfo.entrySet()) {
            Object key = entry.getKey();
            ReplicaInfo replicaInfo = entry.getValue();
            updateSecondaries(key, replicaInfo);
        }
    }
    
    public void updateSecondariesFollowingLeavingPeer(Peer leavingPeer) {
        Map<Object, ReplicaInfo> tmpKeyToReplicaInfo;
        synchronized (keyToReplicaInfo) {
            tmpKeyToReplicaInfo = new HashMap<Object, ReplicaInfo>(keyToReplicaInfo);
        }
        for (Map.Entry<Object, ReplicaInfo> entry : tmpKeyToReplicaInfo.entrySet()) {
            Object key = entry.getKey();
            ReplicaInfo replicaInfo = entry.getValue();
            updateSecondariesFollowingLeavingPeer(key, replicaInfo, leavingPeer);
        }
    }
    
    public ReplicaInfo updateSecondariesFollowingRestoreFromSecondary(Object key, ReplicaInfo replicaInfo) {
        return updateSecondaries(key, replicaInfo);
    }

    protected ReplicaInfo updateSecondaries(Object key, ReplicaInfo replicaInfo) {
        Peer oldSecondaries[] = replicaInfo.getSecondaries();
        Peer newSecondaries[] = backingStrategy.reElectSecondaries(key, replicaInfo.getPrimary(), oldSecondaries);
        replicaInfo = new ReplicaInfo(replicaInfo, localPeer, newSecondaries);
        synchronized (keyToReplicaInfo) {
            keyToReplicaInfo.put(key, replicaInfo);
        }
        
        updateSecondaries(key, replicaInfo, oldSecondaries);
        return replicaInfo;
    }
    
    protected ReplicaInfo updateSecondariesFollowingLeavingPeer(Object key,
            ReplicaInfo replicaInfo,
            Peer leavingPeer) {
        Peer oldSecondaries[] = replicaInfo.getSecondaries();
        
        Peer newSecondaries[] = backingStrategy.reElectSecondaries(key, replicaInfo.getPrimary(), oldSecondaries);
        replicaInfo = new ReplicaInfo(replicaInfo, localPeer, newSecondaries);
        synchronized (keyToReplicaInfo) {
            keyToReplicaInfo.put(key, replicaInfo);
        }
        
        oldSecondaries = filterLeavingPeer(leavingPeer, oldSecondaries);
        updateSecondaries(key, replicaInfo, oldSecondaries);
        return replicaInfo;
    }

    protected Peer[] filterLeavingPeer(Peer leavingPeer, Peer[] oldSecondaries) {
        boolean unchanged = true;
        for (int i = 0; i < oldSecondaries.length; i++) {
            if (oldSecondaries[i].equals(leavingPeer)) {
                oldSecondaries[i] = null;
                unchanged = false;
                break;
            }
        }
        if (unchanged) {
            return oldSecondaries;
        }

        Peer tmpOldSecondaries[] = new Peer[oldSecondaries.length - 1];
        int j = 0;
        for (int i = 0; i < oldSecondaries.length; i++) {
            Peer oldSecondary = oldSecondaries[i];
            if (null != oldSecondary) {
                tmpOldSecondaries[j++] = oldSecondary;
            }
        }
        return tmpOldSecondaries;
    }
    
    protected void updateSecondaries(Object key, ReplicaInfo replicaInfo, Peer[] oldSecondaries) {
        StorageCommandBuilder commandBuilder = new StorageCommandBuilder(key, replicaInfo, oldSecondaries, stateHandler);
        StorageCommand[] commands = commandBuilder.build();
        for (int i = 0; i < commands.length; i++) {
            StorageCommand command = commands[i];
            command.execute(replicaStorageServiceProxy);
        }
    }
    
}