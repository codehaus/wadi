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

import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.InternalReplicationManagerException;
import org.codehaus.wadi.replication.manager.ReplicationKeyAlreadyExistsException;
import org.codehaus.wadi.replication.manager.ReplicationKeyNotFoundException;
import org.codehaus.wadi.replication.manager.ReplicationManager;

/**
 * 
 * @version $Revision: 1538 $
 */
public class NoOpReplicationManager implements ReplicationManager {

    public Object acquirePrimary(Object key) throws ReplicationKeyNotFoundException, InternalReplicationManagerException {
        return null;
    }

    public void create(Object key, Object tmp) throws ReplicationKeyAlreadyExistsException, InternalReplicationManagerException {
    }

    public void destroy(Object key) {
    }

    public boolean managePrimary(Object key) {
        return false;
    }

    public boolean releasePrimary(Object key) {
        return false;
    }

    public ReplicaInfo retrieveReplicaInfo(Object key) throws ReplicationKeyNotFoundException {
        return null;
    }

    public void update(Object key, Object tmp) throws ReplicationKeyNotFoundException, InternalReplicationManagerException {
    }

    public void start() throws Exception {
    }

    public void stop() throws Exception {
    }

}