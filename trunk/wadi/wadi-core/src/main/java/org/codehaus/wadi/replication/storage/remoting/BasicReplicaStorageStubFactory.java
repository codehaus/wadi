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
package org.codehaus.wadi.replication.storage.remoting;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.replication.common.NodeInfo;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.storage.ReplicaStorageStubFactory;


/**
 * 
 * @version $Revision$
 */
public class BasicReplicaStorageStubFactory implements ReplicaStorageStubFactory {
    protected final Dispatcher dispatcher;
    
    public BasicReplicaStorageStubFactory(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public ReplicaStorage buildStub(NodeInfo[] nodes) {
        Address destinations[] = new Address[nodes.length];
        for (int i = 0; i < destinations.length; i++) {
            NodeInfo nodeInfo = nodes[i];
            destinations[i] = dispatcher.getAddress(nodeInfo.getName());
        }
        return newStub(destinations);
    }

    public ReplicaStorage buildStub() {
        Address destinations[] =
            new Address[] {dispatcher.getClusterAddress()};
        return newStub(destinations);
    }
    
    protected ReplicaStorage newStub(Address[] destinations) {
        return new BasicReplicaStorageStub(dispatcher, destinations);
    }
}