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
package org.codehaus.wadi.replication.manager.remoting;

import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.manager.ReplicationManagerStubFactory;
import org.codehaus.wadi.replication.storage.remoting.BasicReplicaStorageStubFactory;

/**
 * 
 * @version $Revision$
 */
public class BasicReplicationManagerStubFactory implements ReplicationManagerStubFactory {
    private final Dispatcher dispatcher;
    
    public BasicReplicationManagerStubFactory(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public ReplicationManager buildStub() {
        return new BasicReplicationManagerStub(dispatcher,
                new BasicReplicaStorageStubFactory(dispatcher));
    }
}
