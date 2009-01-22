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

import java.io.IOException;
import java.util.HashMap;

import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMLocalPeer;
import org.codehaus.wadi.group.vm.VMPeer;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.strategy.BackingStrategy;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class SyncSecondaryManagerTest extends RMockTestCase {

    private HashMap<Object, ReplicaInfo> keyToReplicaInfo;
    private VMLocalPeer localPeer;
    private VMPeer peer2;
    private VMPeer peer3;
    private BackingStrategy backingStrategy;
    private ObjectStateHandler stateHandler;
    private ServiceProxyFactory replicaStorageServiceProxy;
    private SyncSecondaryManager manager;
    private String key;
    private ReplicaInfo replicaInfoForKey;

    @Override
    protected void setUp() throws Exception {
        localPeer = new VMLocalPeer("peer1");
        peer2 = new VMPeer("peer2", null);
        peer3 = new VMPeer("peer3", null);

        keyToReplicaInfo = new HashMap<Object, ReplicaInfo>();
        key = "key";
        Motable motable = (Motable) mock(Motable.class);
        replicaInfoForKey = new ReplicaInfo(localPeer, new Peer[] {peer2, peer3}, motable);
        keyToReplicaInfo.put(key, replicaInfoForKey);

        replicaStorageServiceProxy = (ServiceProxyFactory) mock(ServiceProxyFactory.class);
        backingStrategy = (BackingStrategy) mock(BackingStrategy.class);
        stateHandler = (ObjectStateHandler) mock(ObjectStateHandler.class);

        manager = (SyncSecondaryManager) intercept(SyncSecondaryManager.class, new Object[] {keyToReplicaInfo,
                backingStrategy,
                localPeer,
                stateHandler,
                replicaStorageServiceProxy}, "SyncSecondaryManager");
    }
    
    public void testUpdateSecondariesFollowingJoiningPeer() throws Exception {
        backingStrategy.reElectSecondaries(key, localPeer, replicaInfoForKey.getSecondaries(), null);
        final Peer[] newSecondaries = new Peer[] {peer2, peer3};
        modify().returnValue(newSecondaries);
        
        manager.updateSecondaries(key, replicaInfoForKey, replicaInfoForKey.getSecondaries());
        modify().args(is.AS_RECORDED, new AbstractExpression() {
            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }

            public boolean passes(Object arg0) {
                ReplicaInfo newReplicaInfo = (ReplicaInfo) arg0;
                assertEquals(newSecondaries, newReplicaInfo.getSecondaries());
                assertReplicaInfoMapHasNotYetBeenUpdated();
                return true;
            }
        }, is.AS_RECORDED);
        
        startVerification();
        
        manager.updateSecondariesFollowingJoiningPeer(peer3);
        assertReplicaInfoMapHasNowBeenUpdated();
    }
    
    public void testUpdateSecondariesFollowingLeavingPeer() throws Exception {
        backingStrategy.reElectSecondaries(key, localPeer, replicaInfoForKey.getSecondaries(), null);
        final Peer[] newSecondaries = new Peer[0];
        modify().returnValue(newSecondaries);
        
        manager.updateSecondaries(key, replicaInfoForKey, new Peer[] {peer3});
        modify().args(is.AS_RECORDED, new AbstractExpression() {
            public void describeWith(ExpressionDescriber arg0) throws IOException {
            }
            
            public boolean passes(Object arg0) {
                ReplicaInfo newReplicaInfo = (ReplicaInfo) arg0;
                assertEquals(newSecondaries, newReplicaInfo.getSecondaries());
                assertReplicaInfoMapHasNotYetBeenUpdated();
                return true;
            }
        }, is.AS_RECORDED);
        
        startVerification();

        manager.updateSecondariesFollowingLeavingPeer(peer2);
        assertReplicaInfoMapHasNowBeenUpdated();
    }

    private void assertReplicaInfoMapHasNowBeenUpdated() {
        assertNotSame(keyToReplicaInfo.get(key), replicaInfoForKey);
    }

    private void assertReplicaInfoMapHasNotYetBeenUpdated() {
        assertSame(keyToReplicaInfo.get(key), replicaInfoForKey);
    }
    
}
