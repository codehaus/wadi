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

import java.util.Collections;
import java.util.Set;

import org.codehaus.wadi.core.reflect.base.DeclaredMemberFilter;
import org.codehaus.wadi.core.reflect.jdk.JDKClassIndexerRegistry;
import org.codehaus.wadi.core.util.SimpleStreamer;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceMonitor;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpaceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceSpaceListener;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

import com.agical.rmock.core.Action;
import com.agical.rmock.core.MethodHandle;


/**
 * 
 * @version $Revision: $
 */
public class BasicServiceSpaceTest extends AbstractServiceSpaceTestCase {

    private Dispatcher serviceSpaceDispatcher;
    private Cluster serviceSpaceCluster;
    private StartableServiceRegistry serviceRegistry;
    private ServiceMonitor serviceMonitor;
    private ServiceEndpoint lifecycleEndpoint;
    private ServiceSpaceListener listener;
    private ClusterListener clusterListener;

    protected void setUp() throws Exception {
        super.setUp();

        serviceSpaceCluster = (Cluster) mock(Cluster.class);
        serviceSpaceCluster.getLocalPeer();
        modify().multiplicity(expect.from(0));
        modify().returnValue(localPeer);
        
        serviceSpaceDispatcher = (Dispatcher) mock(Dispatcher.class);
        
        serviceSpaceDispatcher.addInterceptor(null);
        modify().args(is.NOT_NULL);
        
        serviceSpaceDispatcher.register(null);
        modify().multiplicity(expect.exactly(2)).args(is.NOT_NULL);
        
        serviceSpaceDispatcher.getCluster();
        modify().multiplicity(expect.from(0));
        modify().returnValue(serviceSpaceCluster);
        
        serviceRegistry = (StartableServiceRegistry) mock(StartableServiceRegistry.class);
        serviceMonitor = (ServiceMonitor) mock(ServiceMonitor.class);
        listener = (ServiceSpaceListener) mock(ServiceSpaceListener.class);
    }
    
    public void testSuccessfulStart() throws Exception {
        recordStartPhase();
        
        startVerification();
        
        SwapMockBasicServiceSpace serviceSpace = new SwapMockBasicServiceSpace(serviceSpaceName, dispatcher);
        serviceSpace.start();
    }

    public void testDispatcherStartFailure() throws Exception {
        beginSection(s.ordered("Start Phase with Dispatcher failure"));
        recordEndPointsRegistration();
        recordSendToCluster(LifecycleState.STARTING);
        
        serviceSpaceDispatcher.start();
        MessageExchangeException expectedException = new MessageExchangeException("");
        modify().throwException(expectedException);

        recordSendToCluster(LifecycleState.FAILED);
        recordEndPointsUnregistration();
        endSection();
        
        startVerification();
        
        SwapMockBasicServiceSpace serviceSpace = new SwapMockBasicServiceSpace(serviceSpaceName, dispatcher);
        try {
            serviceSpace.start();
            fail();
        } catch (Exception e) {
            assertSame(expectedException, e);
        }
    }

    public void testSuccessfulStop() throws Exception {
        recordStartPhase();

        beginSection(s.ordered("Get and Start ServiceMonitor and Stop ServiceSpace"));
        serviceMonitor.start();
        
        beginSection(s.ordered("Stop Phase"));
        serviceRegistry.stop();
        recordSendToCluster(LifecycleState.STOPPING);
        serviceMonitor.isStarted();
        modify().returnValue(true);
        serviceMonitor.stop();
        serviceSpaceDispatcher.stop();
        recordSendToCluster(LifecycleState.STOPPED);
        recordEndPointsUnregistration();
        endSection();
        
        endSection();
        
        startVerification();
        
        SwapMockBasicServiceSpace serviceSpace = new SwapMockBasicServiceSpace(serviceSpaceName, dispatcher);
        serviceSpace.start();
        
        ServiceMonitor serviceMonitor = serviceSpace.getServiceMonitor(new ServiceName("name1"));
        serviceMonitor.start();
        serviceSpace.stop();
    }

    public void testServiceRegistryAndDispatcherStopFailure() throws Exception {
        recordStartPhase();
        
        beginSection(s.ordered("Stop Phase with ServiceRegistry and Dispatcher failure"));
        serviceRegistry.stop();
        modify().throwException(new Exception());

        recordSendToCluster(LifecycleState.STOPPING);

        serviceSpaceDispatcher.stop();
        modify().throwException(new MessageExchangeException(""));

        recordSendToCluster(LifecycleState.FAILED);
        recordEndPointsUnregistration();
        endSection();
        
        startVerification();
        
        SwapMockBasicServiceSpace serviceSpace = new SwapMockBasicServiceSpace(serviceSpaceName, dispatcher);
        serviceSpace.start();
        serviceSpace.stop();
    }
    
    public void testIgnoreLifecycleEventFromLocalPeer() throws Exception {
        recordStartPhase();
        
        ServiceSpaceLifecycleEvent event = new ServiceSpaceLifecycleEvent(serviceSpaceName, localPeer, 
            LifecycleState.STARTED);
        Envelope message = recordEnvelope(event);
        
        startVerification();
        
        SwapMockBasicServiceSpace serviceSpace = new SwapMockBasicServiceSpace(serviceSpaceName, dispatcher);
        serviceSpace.start();
        serviceSpace.addServiceSpaceListener(listener);
        
        message.setPayload(event);
        assertTrue(lifecycleEndpoint.testDispatchEnvelope(message));
        lifecycleEndpoint.dispatch(message);
        
        Set hostingPeers = serviceSpace.getHostingPeers();
        assertTrue(hostingPeers.isEmpty());
    }
    
    public void testProcessStartedLifecycleEvent() throws Exception {
        executeProcessAddPeerLifecycleEvent(LifecycleState.STARTED);
    }

    public void testProcessAvailableLifecycleEvent() throws Exception {
        executeProcessAddPeerLifecycleEvent(LifecycleState.AVAILABLE);
    }

    public void testProcessStoppingLifecycleEvent() throws Exception {
        executeProcessRemovePeerLifecycleEvent(LifecycleState.STOPPING);
    }

    public void testProcessFailedLifecycleEvent() throws Exception {
        executeProcessRemovePeerLifecycleEvent(LifecycleState.FAILED);
    }
    
    public void testProcessStartingLifecycleEvent() throws Exception {
        recordStartPhase();

        beginSection(s.ordered("STARTING"));
        ServiceSpaceLifecycleEvent event = new ServiceSpaceLifecycleEvent(serviceSpaceName, remote1, 
                LifecycleState.STARTING);
        Envelope message = recordEnvelope(event);
        
        serviceSpaceDispatcher.send(address1, new ServiceSpaceLifecycleEvent(serviceSpaceName, localPeer,
                LifecycleState.AVAILABLE));
        
        listener.receive(event, Collections.EMPTY_SET);
        endSection();
        
        startVerification();
        
        SwapMockBasicServiceSpace serviceSpace = new SwapMockBasicServiceSpace(serviceSpaceName, dispatcher);
        serviceSpace.start();
        serviceSpace.addServiceSpaceListener(listener);
        
        message.setPayload(event);
        assertTrue(lifecycleEndpoint.testDispatchEnvelope(message));
        lifecycleEndpoint.dispatch(message);
        
        Set hostingPeers = serviceSpace.getHostingPeers();
        assertTrue(hostingPeers.isEmpty());
    }

    public void testNodeFailure() throws Exception {
        recordStartPhase();

        beginSection(s.ordered("STARTED "));
        ServiceSpaceLifecycleEvent startedEvent = new ServiceSpaceLifecycleEvent(serviceSpaceName, remote1, 
                LifecycleState.STARTED);
        Envelope startedEnvelope = recordReceiveEvent(startedEvent, false);
        
        ServiceSpaceLifecycleEvent event = new ServiceSpaceLifecycleEvent(serviceSpaceName, remote1,
                LifecycleState.FAILED);
        listener.receive(event, Collections.EMPTY_SET);
        endSection();
        
        startVerification();
        
        SwapMockBasicServiceSpace serviceSpace = new SwapMockBasicServiceSpace(serviceSpaceName, dispatcher);
        serviceSpace.start();
        serviceSpace.addServiceSpaceListener(listener);
        
        startedEnvelope.setPayload(startedEvent);
        assertTrue(lifecycleEndpoint.testDispatchEnvelope(startedEnvelope));
        lifecycleEndpoint.dispatch(startedEnvelope);
        
        Set hostingPeers = serviceSpace.getHostingPeers();
        assertEquals(1, hostingPeers.size());
        assertTrue(hostingPeers.contains(remote1));

        clusterListener.onMembershipChanged(cluster, Collections.EMPTY_SET, Collections.singleton(remote1));
        hostingPeers = serviceSpace.getHostingPeers();
        assertTrue(hostingPeers.isEmpty());
    }
    
    private void executeProcessRemovePeerLifecycleEvent(LifecycleState state) throws Exception {
        recordStartPhase();

        beginSection(s.ordered("STARTED -> " + state));
        ServiceSpaceLifecycleEvent startedEvent = new ServiceSpaceLifecycleEvent(serviceSpaceName, remote1, 
                LifecycleState.STARTED);
        Envelope startedMessage = recordReceiveEvent(startedEvent, false);
        
        ServiceSpaceLifecycleEvent event = new ServiceSpaceLifecycleEvent(serviceSpaceName, remote1, state);
        Envelope message = recordReceiveEvent(event, true);
        endSection();
        
        startVerification();
        
        SwapMockBasicServiceSpace serviceSpace = new SwapMockBasicServiceSpace(serviceSpaceName, dispatcher);
        serviceSpace.start();
        serviceSpace.addServiceSpaceListener(listener);
        
        startedMessage.setPayload(startedEvent);
        assertTrue(lifecycleEndpoint.testDispatchEnvelope(startedMessage));
        lifecycleEndpoint.dispatch(startedMessage);
        
        Set hostingPeers = serviceSpace.getHostingPeers();
        assertEquals(1, hostingPeers.size());
        assertTrue(hostingPeers.contains(remote1));
        
        message.setPayload(event);
        assertTrue(lifecycleEndpoint.testDispatchEnvelope(message));
        lifecycleEndpoint.dispatch(message);
        
        hostingPeers = serviceSpace.getHostingPeers();
        assertTrue(hostingPeers.isEmpty());
    }

    private void executeProcessAddPeerLifecycleEvent(LifecycleState state) throws Exception {
        recordStartPhase();
        
        ServiceSpaceLifecycleEvent event = new ServiceSpaceLifecycleEvent(serviceSpaceName, remote1, state);
        Envelope message = recordReceiveEvent(event, false);
        
        startVerification();
        
        SwapMockBasicServiceSpace serviceSpace = new SwapMockBasicServiceSpace(serviceSpaceName, dispatcher);
        serviceSpace.start();
        serviceSpace.addServiceSpaceListener(listener);
        
        message.setPayload(event);
        assertTrue(lifecycleEndpoint.testDispatchEnvelope(message));
        lifecycleEndpoint.dispatch(message);
        
        Set hostingPeers = serviceSpace.getHostingPeers();
        assertEquals(1, hostingPeers.size());
        assertTrue(hostingPeers.contains(remote1));
    }
    
    private Envelope recordReceiveEvent(ServiceSpaceLifecycleEvent event, boolean newHostingPeersEmpty) {
        Envelope message = recordEnvelope(event);
        listener.receive(event, 
                newHostingPeersEmpty ? Collections.EMPTY_SET : Collections.singleton(event.getHostingPeer()));
        return message;
    }

    private Envelope recordEnvelope(ServiceSpaceLifecycleEvent event) {
        Envelope startedMessage = (Envelope) mock(Envelope.class);
        startedMessage.setPayload(event);
        startedMessage.getPayload();
        modify().multiplicity(expect.from(0));
        modify().returnValue(event);
        return startedMessage;
    }

    private void recordStartPhase() throws MessageExchangeException, Exception {
        beginSection(s.ordered("Start Phase"));
        recordEndPointsRegistration();
        recordSendToCluster(LifecycleState.STARTING);
        serviceSpaceDispatcher.start();
        recordSendToCluster(LifecycleState.STARTED);
        serviceRegistry.start();
        endSection();
    }
    
    private void recordEndPointsUnregistration() {
        dispatcher.unregister(null, 0, 0);
        modify().args(is.NOT_NULL, is.ANYTHING, is.ANYTHING);

        cluster.removeClusterListener(null);
        modify().args(is.NOT_NULL);

        serviceSpaceDispatcher.unregister(null, 0, 0);
        modify().args(is.ANYTHING, is.ANYTHING, is.ANYTHING);
        
        serviceSpaceDispatcher.unregister(null, 0, 0);
        modify().args(is.ANYTHING, is.ANYTHING, is.ANYTHING);
    }

    private void recordEndPointsRegistration() {
        beginSection(s.unordered("Register EndPoints"));
        
        serviceSpaceDispatcher.register(null);
        modify().args(is.NOT_NULL);
        
        serviceSpaceDispatcher.register(null);
        modify().args(is.NOT_NULL);
        modify().perform(new Action() {
            public Object invocation(Object[] arguments, MethodHandle methodHandle) throws Throwable {
                lifecycleEndpoint = (ServiceEndpoint) arguments[0];
                return null;
            }
        });
        
        cluster.addClusterListener(null);
        modify().args(is.NOT_NULL);
        modify().perform(new Action() {
            public Object invocation(Object[] arguments, MethodHandle methodHandle) throws Throwable {
                clusterListener = (ClusterListener) arguments[0];
                return null;
            }
        });

        dispatcher.register(null);
        modify().args(is.NOT_NULL);
        
        endSection();
    }
    
    private void recordSendToCluster(LifecycleState state) throws MessageExchangeException {
        beginSection(s.ordered("Create message and send with ServiceSpaceName"));
        Envelope message = (Envelope) mock(Envelope.class);
        
        dispatcher.createEnvelope();
        modify().returnValue(message);
        
        beginSection(s.unordered("Set message fields"));
        message.setProperty(BasicServiceSpaceEnvelopeHelper.PROPERTY_SERVICE_SPACE_NAME, serviceSpaceName);
        
        ServiceSpaceLifecycleEvent event = newLifecycleEvent(state);
        message.setPayload(event);
        
        message.setReplyTo(localPeer.getAddress());
        message.setAddress(clusterAddress);
        endSection();
        
        dispatcher.send(clusterAddress, message);
        endSection();
    }

    private ServiceSpaceLifecycleEvent newLifecycleEvent(LifecycleState state) {
        return new ServiceSpaceLifecycleEvent(serviceSpaceName, localPeer, state);
    }

    private class SwapMockBasicServiceSpace extends BasicServiceSpace {

        public SwapMockBasicServiceSpace(ServiceSpaceName name, Dispatcher underlyingDispatcher) {
            super(name,
                underlyingDispatcher,
                new JDKClassIndexerRegistry(new DeclaredMemberFilter()),
                new SimpleStreamer());
        }

        protected Dispatcher newDispatcher() {
            return serviceSpaceDispatcher;
        }
        
        protected StartableServiceRegistry newServiceRegistry() {
            return serviceRegistry;
        }
        
        protected ServiceMonitor newServiceMonitor(ServiceName serviceName) {
            return serviceMonitor;
        }
        
    }
    
}
