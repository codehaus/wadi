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

import org.codehaus.wadi.replication.Replicater;
import org.codehaus.wadi.replication.ReplicaterFactory;
import org.codehaus.wadi.replication.manager.basic.MotableReplicationManager;

/**
 * 
 * @version $Revision$
 */
public class ReplicaterAdapterFactory implements ReplicaterFactory {
    private final ReplicationManager replicationManager;
    
    public ReplicaterAdapterFactory(ReplicationManager replicationManager) {
        if (null == replicationManager) {
            throw new IllegalArgumentException("replicationManager is required");
        }
        this.replicationManager = replicationManager;
    }

    public Replicater create() {
        ReplicationManager sessionRepManager = new MotableReplicationManager(replicationManager);
        return new ReplicaterAdapter(sessionRepManager);
    }
    
}
