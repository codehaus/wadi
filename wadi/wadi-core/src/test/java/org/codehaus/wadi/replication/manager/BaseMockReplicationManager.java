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

import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.ReplicationManager;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BaseMockReplicationManager implements ReplicationManager {
    
    public void create(Object key, Object tmp) {
        throw new UnsupportedOperationException();
    }

    public void update(Object key, Object tmp) {
        throw new UnsupportedOperationException();
    }

    public void destroy(Object key) {
        throw new UnsupportedOperationException();
    }

    public Object acquirePrimary(Object key) {
        throw new UnsupportedOperationException();
    }

    public ReplicaInfo releasePrimary(Object key) {
        throw new UnsupportedOperationException();
    }

    public ReplicaInfo retrieveReplicaInfo(Object key) {
        throw new UnsupportedOperationException();
    }

    public boolean managePrimary(Object key) {
        throw new UnsupportedOperationException();
    }

    public void start() throws Exception {
        throw new UnsupportedOperationException();
    }

    public void stop() throws Exception {
        throw new UnsupportedOperationException();
    }
}