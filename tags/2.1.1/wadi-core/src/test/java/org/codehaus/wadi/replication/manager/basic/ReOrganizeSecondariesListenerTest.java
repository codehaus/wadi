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

import java.net.URI;
import java.util.Collections;

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.replication.storage.ReplicaStorage;
import org.codehaus.wadi.replication.strategy.BackingStrategy;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceListener;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class ReOrganizeSecondariesListenerTest extends RMockTestCase {

    private ServiceSpaceName serviceSpaceName;
    private Peer peer1;
    private Peer peer2;
    private BackingStrategy backingStrategy;
    private SecondaryManager secondaryManager;

    @Override
    protected void setUp() throws Exception {
        serviceSpaceName = new ServiceSpaceName(new URI("name"));
        peer1 = (Peer) mock(Peer.class);
        peer2 = (Peer) mock(Peer.class);
        backingStrategy = (BackingStrategy) mock(BackingStrategy.class);
        secondaryManager = (SecondaryManager) mock(SecondaryManager.class);
    }
    
    public void testStorageListener() throws Exception {
        beginSection(s.ordered("ordered secondary un/registration"));
        backingStrategy.addSecondary(peer1);
        secondaryManager.updateSecondariesFollowingJoiningPeer(peer1);
        
        backingStrategy.addSecondary(peer2);
        secondaryManager.updateSecondariesFollowingJoiningPeer(peer2);

        backingStrategy.removeSecondary(peer1);
        secondaryManager.updateSecondariesFollowingLeavingPeer(peer1);
        
        backingStrategy.removeSecondary(peer2);
        secondaryManager.updateSecondariesFollowingLeavingPeer(peer2);
        endSection();
        startVerification();
        
        ServiceListener listener = new ReOrganizeSecondariesListener(backingStrategy, secondaryManager);
        
        receiveEvent(listener, peer1, LifecycleState.AVAILABLE);
        receiveEvent(listener, peer2, LifecycleState.STARTED);
        receiveEvent(listener, peer1, LifecycleState.STOPPING);
        receiveEvent(listener, peer2, LifecycleState.FAILED);
    }

    private void receiveEvent(ServiceListener listener, Peer peer, LifecycleState state) {
        listener.receive(new ServiceLifecycleEvent(serviceSpaceName, ReplicaStorage.NAME, peer, state), 
                Collections.EMPTY_SET);
    }

}
