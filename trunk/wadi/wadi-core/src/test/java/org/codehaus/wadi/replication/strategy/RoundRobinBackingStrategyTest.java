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
package org.codehaus.wadi.replication.strategy;

import java.util.Arrays;
import java.util.HashSet;

import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMPeer;
import org.codehaus.wadi.servicespace.ServiceSpace;

import com.agical.rmock.extension.junit.RMockTestCase;

public class RoundRobinBackingStrategyTest extends RMockTestCase {
    private RoundRobinBackingStrategy strategy;
    private Peer peer1;
    private Peer peer2;
    private Peer peer3;
    private Peer peer4;
    private LocalPeer localPeer;

    protected void setUp() throws Exception {
        ServiceSpace serviceSpace = (ServiceSpace) mock(ServiceSpace.class);
        localPeer = serviceSpace.getLocalPeer();
        startVerification();
        
        strategy = new RoundRobinBackingStrategy(serviceSpace, 2);
        peer1 = new VMPeer("peer1", null);
        peer2 = new VMPeer("peer2", null);
        peer3 = new VMPeer("peer3", null);
        peer4 = new VMPeer("peer4", null);
    }
    
    public void testLocalPeerIsNotIgnoredByAddSecondaries() {
        strategy.addSecondaries(new Peer[] {localPeer});
        
        Peer[] actualSecondaries = strategy.electSecondaries(null);
        assertSecondaries(new Peer[0], actualSecondaries);
   }

    public void testLocalPeerIsNotIgnoredByAddSecondary() {
        strategy.addSecondary(localPeer);
        
        Peer[] actualSecondaries = strategy.electSecondaries(null);
        assertSecondaries(new Peer[0], actualSecondaries);
    }
    
    public void testElectSecondaries() {
        strategy.addSecondaries(new Peer[] {peer1, peer2, peer3, peer4});
        
        Peer[] actualSecondaries = strategy.electSecondaries(null);
        assertSecondaries(new Peer[] {peer1, peer2}, actualSecondaries);
        
        actualSecondaries = strategy.electSecondaries(null);
        assertSecondaries(new Peer[] {peer3, peer4}, actualSecondaries);
        
        strategy.removeSecondary(peer2);
        
        actualSecondaries = strategy.electSecondaries(null);
        assertSecondaries(new Peer[] {peer1, peer3}, actualSecondaries);
        
        actualSecondaries = strategy.electSecondaries(null);
        assertSecondaries(new Peer[] {peer4, peer1}, actualSecondaries);
    }
    
    private void assertSecondaries(Peer[] expectedPeers, Peer[] actualPeers) {
        assertEquals(new HashSet(Arrays.asList(expectedPeers)), new HashSet(Arrays.asList(actualPeers)));
    }
    
}
