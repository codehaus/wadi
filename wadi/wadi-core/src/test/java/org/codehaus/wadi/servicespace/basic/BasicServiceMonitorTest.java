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

import java.util.Set;

import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.group.vm.VMEnvelope;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceListener;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpaceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceSpaceListener;

import com.agical.rmock.core.Action;
import com.agical.rmock.core.MethodHandle;


/**
 * 
 * @version $Revision: $
 */
public class BasicServiceMonitorTest extends AbstractServiceSpaceTestCase {

    private ServiceName serviceName;
    private ServiceEndpoint endpoint;
    private ServiceSpaceListener serviceSpaceListener;

    protected void setUp() throws Exception {
        super.setUp();
        serviceName = new ServiceName("name");
    }
    
    public void testStartMulticastQueryEvent() throws Exception {
        recordStartPhase();
        
        startVerification();
        
        BasicServiceMonitor monitor = new BasicServiceMonitor(serviceSpace, serviceName);
        monitor.start();
    }

    public void testReceiveAvailableEvent() throws Exception {
        executeReceiveEventStateShouldAddHostingPeer(LifecycleState.AVAILABLE);
    }

    public void testReceiveStartedEvent() throws Exception {
        executeReceiveEventStateShouldAddHostingPeer(LifecycleState.STARTED);
    }

    public void testReceiveStoppedEvent() throws Exception {
        recordStartPhase();

        ServiceLifecycleEvent eventStopped = newServiceLifecycleEvent(remote1, LifecycleState.STOPPED);
        ServiceListener listener = (ServiceListener) mock(ServiceListener.class);
        listener.receive(eventStopped);
        
        startVerification();
        
        BasicServiceMonitor monitor = new BasicServiceMonitor(serviceSpace, serviceName);
        monitor.start();
        
        Set hostingPeers = monitor.getHostingPeers();
        assertEquals(0, hostingPeers.size());

        VMEnvelope message = new VMEnvelope();
        message.setPayload(newServiceLifecycleEvent(remote1, LifecycleState.STARTED));
        endpoint.dispatch(message);
        
        monitor.addServiceLifecycleListener(listener);
        
        message.setPayload(eventStopped);
        endpoint.dispatch(message);
        
        hostingPeers = monitor.getHostingPeers();
        assertTrue(hostingPeers.isEmpty());
    }

    public void testNotifyHostingPeerFailure() throws Exception {
        recordStartPhase();

        ServiceLifecycleEvent eventStopped = newServiceLifecycleEvent(remote1, LifecycleState.FAILED);
        ServiceListener listener = (ServiceListener) mock(ServiceListener.class);
        listener.receive(eventStopped);
        
        startVerification();
        
        BasicServiceMonitor monitor = new BasicServiceMonitor(serviceSpace, serviceName);
        monitor.start();
        

        VMEnvelope message = new VMEnvelope();
        message.setPayload(newServiceLifecycleEvent(remote1, LifecycleState.STARTED));
        endpoint.dispatch(message);
        
        Set hostingPeers = monitor.getHostingPeers();
        assertEquals(1, hostingPeers.size());

        monitor.addServiceLifecycleListener(listener);
        
        serviceSpaceListener.receive(new ServiceSpaceLifecycleEvent(serviceSpaceName, remote1, LifecycleState.FAILED));
        
        hostingPeers = monitor.getHostingPeers();
        assertTrue(hostingPeers.isEmpty());
    }

    public void testNotifyHostingPeerFailureWithPeerNotServiceHost() throws Exception {
        recordStartPhase();

        ServiceListener listener = (ServiceListener) mock(ServiceListener.class);
        
        startVerification();
        
        BasicServiceMonitor monitor = new BasicServiceMonitor(serviceSpace, serviceName);
        monitor.start();
        
        Set hostingPeers = monitor.getHostingPeers();
        assertEquals(0, hostingPeers.size());

        VMEnvelope message = new VMEnvelope();
        message.setPayload(newServiceLifecycleEvent(remote1, LifecycleState.STARTED));
        endpoint.dispatch(message);
        
        monitor.addServiceLifecycleListener(listener);
        
        serviceSpaceListener.receive(new ServiceSpaceLifecycleEvent(serviceSpaceName, remote2, LifecycleState.FAILED));
        
        hostingPeers = monitor.getHostingPeers();
        assertEquals(1, hostingPeers.size());
    }

    private void executeReceiveEventStateShouldAddHostingPeer(LifecycleState state) throws Exception {
        recordStartPhase();

        ServiceLifecycleEvent eventRemote2 = newServiceLifecycleEvent(remote2, state);
        ServiceListener listener = (ServiceListener) mock(ServiceListener.class);
        listener.receive(eventRemote2);
        
        startVerification();
        
        BasicServiceMonitor monitor = new BasicServiceMonitor(serviceSpace, serviceName);
        monitor.start();
        
        Set hostingPeers = monitor.getHostingPeers();
        assertEquals(0, hostingPeers.size());

        VMEnvelope message = new VMEnvelope();
        message.setPayload(newServiceLifecycleEvent(remote1, state));
        assertTrue(endpoint.testDispatchMessage(message));
        endpoint.dispatch(message);
        
        hostingPeers = monitor.getHostingPeers();
        assertEquals(1, hostingPeers.size());
        assertTrue(hostingPeers.contains(remote1));

        monitor.addServiceLifecycleListener(listener);
        
        message.setPayload(eventRemote2);
        assertTrue(endpoint.testDispatchMessage(message));
        endpoint.dispatch(message);
        
        hostingPeers = monitor.getHostingPeers();
        assertEquals(2, hostingPeers.size());
        assertTrue(hostingPeers.contains(remote1));
        assertTrue(hostingPeers.contains(remote2));
    }
    
    private ServiceLifecycleEvent newServiceLifecycleEvent(Peer peer, LifecycleState state) {
        return new ServiceLifecycleEvent(serviceSpaceName, serviceName, peer, state);
    }
    
    private void recordStartPhase() throws Exception {
        beginSection(s.ordered("Register Dispatcher and multicast ServiceQueryEvent"));
        
        serviceSpace.addServiceSpaceListener(null);
        modify().args(is.NOT_NULL);
        modify().perform(new Action() {

            public Object invocation(Object[] arguments, MethodHandle methodHandle) throws Throwable {
                serviceSpaceListener = (ServiceSpaceListener) arguments[0];
                return null;
            }
            
        });
        
        dispatcher.register(null);
        modify().args(is.NOT_NULL);
        modify().perform(new Action() {

            public Object invocation(Object[] arg0, MethodHandle arg1) throws Throwable {
                endpoint = (ServiceEndpoint) arg0[0];
                return null;
            }
            
        });

        beginSection(s.unordered("Multicast ServiceQueryEvent"));
        dispatcher.send(address1, new ServiceQueryEvent(serviceSpaceName, serviceName, localPeer));
        dispatcher.send(address2, new ServiceQueryEvent(serviceSpaceName, serviceName, localPeer));
        endSection();
        
        endSection();
    }
    
    
}
