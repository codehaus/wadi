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

import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMLocalPeer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.storage.ReplicaStorage;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class UpdateReplicationCommandTest extends RMockTestCase {
    private LocalPeer localPeer;
    private ObjectStateHandler stateHandler;
    private ProxyFactory proxyFactory;
    private HashMap<Object, ReplicaInfo> keyToReplicaInfo;
    private Motable instance;
    private Object key;
    private UpdateReplicationCommand command;

    protected void setUp() throws Exception {
        localPeer = new VMLocalPeer("peer1");
        keyToReplicaInfo = new HashMap<Object, ReplicaInfo>();
        stateHandler = (ObjectStateHandler) mock(ObjectStateHandler.class);
        proxyFactory = (ProxyFactory) mock(ProxyFactory.class);
        key = new Object();
        instance = (Motable) mock(Motable.class);
        
        command = new UpdateReplicationCommand(keyToReplicaInfo, stateHandler, proxyFactory, key, instance); 
    }

    public void testRun() throws Exception {
        Peer[] targets = new Peer[] {localPeer};
        Motable currentInstance = (Motable) mock(Motable.class);

        keyToReplicaInfo.put(key, new ReplicaInfo(localPeer, targets, currentInstance));
        
        beginSection(s.ordered("Extract Update; Reset state; Elect Secondaries"));
        stateHandler.extractUpdatedState(key, instance);
        modify().returnValue(new byte[0]);
        
        stateHandler.resetObjectState(instance);
        
        ReplicaStorage newReplicaStorageProxy = proxyFactory.newReplicaStorageProxy(targets);
        newReplicaStorageProxy.mergeUpdate(key, null);
        modify().args(is.AS_RECORDED, is.ANYTHING);
        
        endSection();
        
        startVerification();
        
        command.run();
    }

}
