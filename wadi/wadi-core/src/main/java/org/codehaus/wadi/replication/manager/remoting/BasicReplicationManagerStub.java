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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.impl.ServiceEndpointBuilder;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.storage.ReplicaStorageStubFactory;


/**
 * 
 * @version $Revision$
 */
public class BasicReplicationManagerStub implements ReplicationManager {
    private static final Log log = LogFactory.getLog(BasicReplicationManagerStub.class);
    
    private final Dispatcher dispatcher;
    private final ReplicaStorageStubFactory storageStubFactory;

    public BasicReplicationManagerStub(Dispatcher dispatcher,
            ReplicaStorageStubFactory storageStubFactory) {
        this.dispatcher = dispatcher;
        this.storageStubFactory = storageStubFactory;

        // TODO - we need to dispose.
        ServiceEndpointBuilder endpointBuilder = new ServiceEndpointBuilder();
        endpointBuilder.addCallback(dispatcher, ReleasePrimaryResult.class);
    }
    
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
        ReleasePrimaryRequest command = new ReleasePrimaryRequest(key);
        Address from = dispatcher.getLocalAddress();
        Address to = dispatcher.getClusterAddress();
        ReplicaInfo info = null;
        try {
            Message message = dispatcher.exchangeSend(from, to, command, ReleasePrimaryRequest.DEFAULT_TWO_WAY_TIMEOUT);
            ReleasePrimaryResult result = (ReleasePrimaryResult) message.getPayload();
            info = (ReplicaInfo) result.getPayload();
        } catch (MessageExchangeException e) {
        }
        
        if (null == info) {
            ReplicaStorage storage = storageStubFactory.buildStub();
            info = storage.retrieveReplicaInfo(key);
        }
        
        return info;
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
