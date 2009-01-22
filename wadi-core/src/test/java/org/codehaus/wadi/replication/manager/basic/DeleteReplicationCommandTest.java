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

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class DeleteReplicationCommandTest extends RMockTestCase {

    private LocalPeer localPeer;
    private ProxyFactory proxyFactory;
    private HashMap<Object, ReplicaInfo> keyToReplicaInfo;
    private Motable instance;
    private Object key;
    private DeleteReplicationCommand command;

    protected void setUp() throws Exception {
        keyToReplicaInfo = new HashMap<Object, ReplicaInfo>();
        proxyFactory = (ProxyFactory) mock(ProxyFactory.class);
        localPeer = new VMLocalPeer("peer1");
        key = new Object();
        instance = (Motable) mock(Motable.class);
        
        command = new DeleteReplicationCommand(keyToReplicaInfo, proxyFactory, key); 
    }

    public void testRun() throws Exception {
        keyToReplicaInfo.put(key, new ReplicaInfo(localPeer, new Peer[0], instance));

        startVerification();
        
        command.run();
        assertFalse(keyToReplicaInfo.containsKey(key));
    }
    
}
