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
package org.codehaus.wadi.servicespace.basic;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.core.reflect.base.DeclaredMemberFilter;
import org.codehaus.wadi.core.reflect.jdk.JDKClassIndexerRegistry;
import org.codehaus.wadi.core.util.SimpleStreamer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.vm.VMBroker;
import org.codehaus.wadi.group.vm.VMDispatcher;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceMonitor;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpaceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceSpaceListener;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ServiceSpaceSmokeTest extends TestCase {

    public void testSmoke() throws Exception {
        ServiceSpaceName serviceSpace1Name = new ServiceSpaceName(new URI("space1"));
        VMBroker broker = new VMBroker("CLUSTER");
        VMDispatcher dispatcher1 = new VMDispatcher(broker, "node1", null);
        dispatcher1.start();
        
        VMDispatcher dispatcher2 = new VMDispatcher(broker, "node2", null);
        dispatcher2.start();
        
        Lifecycle service1 = new MockService();

        Lifecycle service2 = new MockService();
        
        BasicServiceSpace space1OnDisp1 = new BasicServiceSpace(serviceSpace1Name,
            dispatcher1,
            new JDKClassIndexerRegistry(new DeclaredMemberFilter()),
            new SimpleStreamer());
        ServiceName service1Name = new ServiceName("service1");
        space1OnDisp1.getServiceRegistry().register(service1Name, service1);
        space1OnDisp1.start();
        EventTracker spaceTrackerOnDisp1 = new EventTracker();
        space1OnDisp1.addServiceSpaceListener(spaceTrackerOnDisp1);
        
        BasicServiceSpace space1OnDisp2 = new BasicServiceSpace(serviceSpace1Name,
            dispatcher2,
            new JDKClassIndexerRegistry(new DeclaredMemberFilter()),
            new SimpleStreamer());
        space1OnDisp2.getServiceRegistry().register(new ServiceName("service2"), service2);
        
        ServiceMonitor service1Monitor = space1OnDisp2.getServiceMonitor(service1Name);
        space1OnDisp2.start();
 
        // Implementation note: we need to wait for the starting and started events to be delivered.
        Thread.sleep(500);
        
        assertEquals(1, space1OnDisp1.getHostingPeers().size());
        assertEquals(1, space1OnDisp2.getHostingPeers().size());
        
        assertEquals(2, spaceTrackerOnDisp1.received.size());
        ServiceSpaceLifecycleEvent event = (ServiceSpaceLifecycleEvent) spaceTrackerOnDisp1.received.get(0);
        assertTrue(event.getState() == LifecycleState.STARTING);
        event = (ServiceSpaceLifecycleEvent) spaceTrackerOnDisp1.received.get(1);
        assertTrue(event.getState() == LifecycleState.STARTED);
        spaceTrackerOnDisp1.received.clear();
        
        service1Monitor.start();
        // Implementation note: we need to wait for the nodes hosting the monitored service to reply.
        Thread.sleep(500);
        Set hostingPeers = service1Monitor.getHostingPeers();
        assertEquals(1, hostingPeers.size());
        assertEquals("node1", ((Peer) hostingPeers.iterator().next()).getName());
        
        EventTracker spaceTrackerOnDisp2 = new EventTracker();
        space1OnDisp2.addServiceSpaceListener(spaceTrackerOnDisp2);
        
        space1OnDisp1.stop();
        
        // Implementation note: we need to wait for the stopping and stopped events to be delivered.
        Thread.sleep(500);
        assertEquals(0, space1OnDisp1.getHostingPeers().size());
        assertEquals(0, space1OnDisp2.getHostingPeers().size());
        
        assertEquals(2, spaceTrackerOnDisp2.received.size());
        event = (ServiceSpaceLifecycleEvent) spaceTrackerOnDisp2.received.get(0);
        assertTrue(event.getState() == LifecycleState.STOPPING);
        event = (ServiceSpaceLifecycleEvent) spaceTrackerOnDisp2.received.get(1);
        assertTrue(event.getState() == LifecycleState.STOPPED);
        spaceTrackerOnDisp2.received.clear();
        
        hostingPeers = service1Monitor.getHostingPeers();
        assertTrue(hostingPeers.isEmpty());
        
        space1OnDisp1.start();
        
        Thread.sleep(500);
        assertEquals(1, space1OnDisp1.getHostingPeers().size());
        assertEquals(1, space1OnDisp2.getHostingPeers().size());

        hostingPeers = service1Monitor.getHostingPeers();
        assertEquals(1, hostingPeers.size());
        assertEquals("node1", ((Peer) hostingPeers.iterator().next()).getName());
    }
    
    private static class EventTracker implements ServiceSpaceListener {
        private final List<ServiceSpaceLifecycleEvent> received = new ArrayList<ServiceSpaceLifecycleEvent>();
        
        public void receive(ServiceSpaceLifecycleEvent event, Set<Peer> newHostingPeers) {
            received.add(event);
        }
        
    }
    
    private static class MockService implements Lifecycle {

        public void start() throws Exception {
        }

        public void stop() throws Exception {
        }
        
    }
    
}
