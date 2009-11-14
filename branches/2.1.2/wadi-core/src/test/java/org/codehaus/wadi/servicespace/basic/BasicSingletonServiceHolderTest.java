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

package org.codehaus.wadi.servicespace.basic;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.wadi.group.EndPoint;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.PeerInfo;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceHolder;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceSpaceListener;
import org.codehaus.wadi.servicespace.ServiceSpaceName;
import org.codehaus.wadi.servicespace.SingletonService;

import com.agical.rmock.core.describe.ExpressionDescriber;
import com.agical.rmock.core.match.operator.AbstractExpression;
import com.agical.rmock.extension.junit.RMockTestCase;

/**
 *
 * @version $Rev:$ $Date:$
 */
public class BasicSingletonServiceHolderTest extends RMockTestCase {

    private ServiceSpaceListener membershipListener;
    private ServiceSpace serviceSpace;
    private SingletonService singletonService;
    private ServiceHolder delegateServiceHolder;
    private LocalPeer localPeer;
    private EndPoint endPoint;
    private ServiceSpaceName serviceSpaceName;
    private Peer otherPeer;

    @Override
    protected void setUp() throws Exception {
        endPoint = (EndPoint) mock(EndPoint.class);
        
        serviceSpaceName = new ServiceSpaceName(new URI("name"));
        serviceSpace = (ServiceSpace) mock(ServiceSpace.class);

        singletonService = (SingletonService) mock(SingletonService.class);
        delegateServiceHolder = (ServiceHolder) mock(ServiceHolder.class);
    }

    private void recordInitHostingPeer(long birthtimeLocal, long birthtimeOtherPeer) {
        localPeer = serviceSpace.getDispatcher().getCluster().getLocalPeer();
        localPeer.getPeerInfo();
        PeerInfo localPeerInfo = new PeerInfo(endPoint, birthtimeLocal);
        modify().multiplicity(expect.from(0)).returnValue(localPeerInfo);
        localPeer.toString();

        otherPeer = (Peer) mock(Peer.class, "Peer");
        otherPeer.getPeerInfo();
        PeerInfo peerInfo1 = new PeerInfo(endPoint, birthtimeOtherPeer);
        modify().returnValue(peerInfo1);
        
        Set<Peer> hostingPeers = new HashSet<Peer>();
        hostingPeers.add(localPeer);
        hostingPeers.add(otherPeer);
        
        serviceSpace.getHostingPeers();
        modify().returnValue(hostingPeers);
    }

    private BasicSingletonServiceHolder newServiceHolder() {
        ServiceName serviceName = new ServiceName("name");

        return new BasicSingletonServiceHolder(serviceSpace, serviceName, singletonService) {
            @Override
            protected ServiceHolder newDelegateServiceHolder(ServiceSpace serviceSpace,
                ServiceName serviceName,
                Object service) {
                return delegateServiceHolder;
            }
        };
    }
    
    public void testElectionOnStart() throws Exception {
        serviceSpace.addServiceSpaceListener(null);
        modify().args(new MembershipListenerSetter());

        recordInitHostingPeer(0, 1);

        delegateServiceHolder.start();
        
        startVerification();
        
        BasicSingletonServiceHolder serviceHolder = newServiceHolder();
        serviceHolder.start();
    }
    
    public void testReleaseSingletonOnStop() throws Exception {
        serviceSpace.addServiceSpaceListener(null);
        modify().args(new MembershipListenerSetter());

        recordInitHostingPeer(0, 1);

        delegateServiceHolder.start();

        delegateServiceHolder.stop();

        serviceSpace.removeServiceSpaceListener(null);
        modify().args(is.NOT_NULL);
        
        startVerification();
        
        BasicSingletonServiceHolder serviceHolder = newServiceHolder();
        serviceHolder.start();
        serviceHolder.stop();
    }
    
    public void testBecomeSingletonOnMembershipUpdate() throws Exception {
        recordInitHostingPeer(1, 0);

        serviceSpace.addServiceSpaceListener(null);
        modify().args(new MembershipListenerSetter());

        delegateServiceHolder.start();

        singletonService.onBecomeSingletonDueToMembershipUpdate();
        
        startVerification();
        
        BasicSingletonServiceHolder serviceHolder = newServiceHolder();
        serviceHolder.start();
        
        Set<Peer> newHostingPeers = new HashSet<Peer>();
        newHostingPeers.add(localPeer);

        membershipListener.receive(new ServiceSpaceLifecycleEvent(serviceSpaceName, otherPeer, LifecycleState.STOPPED),
            newHostingPeers);
    }
    
    public void testReleaseSingletonOnMembershipUpdate() throws Exception {
        recordInitHostingPeer(1, 2);

        Peer singletonPeer = (Peer) mock(Peer.class, "singletonPeer");
        singletonPeer.getPeerInfo();
        PeerInfo peerInfo = new PeerInfo(endPoint, 0);
        modify().returnValue(peerInfo);
        
        serviceSpace.addServiceSpaceListener(null);
        modify().args(new MembershipListenerSetter());

        delegateServiceHolder.stop();
        
        startVerification();
        
        BasicSingletonServiceHolder serviceHolder = newServiceHolder();
        serviceHolder.start();
        
        Set<Peer> newHostingPeers = new HashSet<Peer>();
        newHostingPeers.add(localPeer);
        newHostingPeers.add(singletonPeer);

        membershipListener.receive(new ServiceSpaceLifecycleEvent(serviceSpaceName, otherPeer, LifecycleState.STOPPED),
            newHostingPeers);
    }
    
    private final class MembershipListenerSetter extends AbstractExpression {
        public void describeWith(ExpressionDescriber arg0) throws IOException {
        }

        public boolean passes(Object arg0) {
            membershipListener = (ServiceSpaceListener) arg0;
            return true;
        }
    }
    
}
