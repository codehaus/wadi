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

import java.util.Set;

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.replication.strategy.BackingStrategy;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceListener;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class ReOrganizeSecondariesListener implements ServiceListener {
    private final BackingStrategy backingStrategy;
    private final SecondaryManager replicaInfoReOrganizer;
    
    public ReOrganizeSecondariesListener(BackingStrategy backingStrategy, SecondaryManager replicaInfoReOrganizer) {
        if (null == backingStrategy) {
            throw new IllegalArgumentException("backingStrategy is required");
        } else if (null == replicaInfoReOrganizer) {
            throw new IllegalArgumentException("replicaInfoReOrganizer is required");
        }
        this.backingStrategy = backingStrategy;
        this.replicaInfoReOrganizer = replicaInfoReOrganizer;
    }

    public void receive(ServiceLifecycleEvent event, Set newHostingPeers) {
        LifecycleState state = event.getState();
        if (state == LifecycleState.AVAILABLE || state == LifecycleState.STARTED) {
            Peer hostingPeer = event.getHostingPeer();
            backingStrategy.addSecondary(hostingPeer);
            replicaInfoReOrganizer.updateSecondariesFollowingJoiningPeer(hostingPeer);
        } else if (state == LifecycleState.STOPPING || state == LifecycleState.FAILED) {
            Peer hostingPeer = event.getHostingPeer();
            backingStrategy.removeSecondary(hostingPeer);
            replicaInfoReOrganizer.updateSecondariesFollowingLeavingPeer(hostingPeer);
        }
    }
}