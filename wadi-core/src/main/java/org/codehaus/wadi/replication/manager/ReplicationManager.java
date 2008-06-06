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
package org.codehaus.wadi.replication.manager;

import java.util.Set;

import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.servicespace.ServiceName;

/**
 * 
 * @version $Revision$
 */
public interface ReplicationManager extends Lifecycle {
    ServiceName NAME = new ServiceName("ReplicationManager");
    
    void create(Object key, Motable tmp) throws ReplicationKeyAlreadyExistsException,
            InternalReplicationManagerException;
    
    void update(Object key, Motable tmp) throws ReplicationKeyNotFoundException, InternalReplicationManagerException;
    
    void destroy(Object key);

    Motable retrieveReplica(Object key) throws ReplicationKeyNotFoundException, InternalReplicationManagerException;

    void insertReplicaInfo(Object key, ReplicaInfo replicaInfo) throws ReplicationKeyAlreadyExistsException;
    
    ReplicaInfo releaseReplicaInfo(Object key, Peer newPrimary) throws ReplicationKeyNotFoundException;
    
    Set<Object> getManagedReplicaInfoKeys();
}
