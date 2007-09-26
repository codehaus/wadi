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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.EnvelopeListener;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceMonitor;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceProxyFactory;
import org.codehaus.wadi.servicespace.ServiceRegistry;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceSpaceListener;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

/**
 * 
 * @version $Revision: $
 */
public class BasicServiceSpace implements ServiceSpace, Lifecycle {
    private static final Log log = LogFactory.getLog(BasicServiceSpace.class);
    
    private final CopyOnWriteArrayList listeners;
    private final Set<Peer> hostingPeers;
    private final Collection<ServiceMonitor> monitors;
    private final LocalPeer localPeer;
    private final StartableServiceRegistry serviceRegistry;
    private final ServiceSpaceName name;
    protected final Dispatcher underlyingDispatcher;
    protected final Dispatcher dispatcher;
    private final ServiceEndpoint serviceSpaceEndpoint;
    private final ServiceEndpoint lifecycleEndpoint;
    private final ServiceEndpoint serviceSpaceRVEndPoint;
    private final ClusterListener underlyingClusterListener;
    private final ServiceSpaceEnvelopeHelper envelopeHelper;

    public BasicServiceSpace(ServiceSpaceName name, Dispatcher underlyingDispatcher) {
        if (null == name) {
            throw new IllegalArgumentException("name is required");
        } else if (null == underlyingDispatcher) {
            throw new IllegalArgumentException("underlyingDispatcher is required");
        }
        monitors = new ArrayList<ServiceMonitor>();
        hostingPeers = new HashSet<Peer>();
        listeners = new CopyOnWriteArrayList();

        this.name = name;
        this.underlyingDispatcher = underlyingDispatcher;
        this.dispatcher = newDispatcher();

        localPeer = dispatcher.getCluster().getLocalPeer();
        serviceRegistry = newServiceRegistry();
        
        underlyingClusterListener = new UnderlyingClusterListener();
        
        EnvelopeListener messageListener = dispatcher; 
        messageListener = new ServiceInvocationListener(this, messageListener);
        messageListener = new ServiceResponseListener(this, messageListener);
        serviceSpaceEndpoint = new ServiceSpaceEndpoint(this, messageListener);
        
        lifecycleEndpoint = new ServiceSpaceLifecycleEndpoint();
        serviceSpaceRVEndPoint = new RendezVousEndPoint(this);
        envelopeHelper = new ServiceSpaceEnvelopeHelper(this);
    }
    
    public LocalPeer getLocalPeer() {
        return localPeer;
    }
    
    public void addServiceSpaceListener(ServiceSpaceListener listener) {
        listeners.add(listener);
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public ServiceSpaceName getServiceSpaceName() {
        return name;
    }
    
    public Set getHostingPeers() {
        synchronized (hostingPeers) {
            return new HashSet(hostingPeers);
        }
    }

    public ServiceMonitor getServiceMonitor(ServiceName serviceName) {
        ServiceMonitor serviceMonitor = newServiceMonitor(serviceName);
        synchronized (monitors) {
            monitors.add(serviceMonitor);
        }
        return serviceMonitor;
    }
    
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    public void removeServiceSpaceListener(ServiceSpaceListener listener) {
        boolean removed = listeners.remove(listener);
        if (!removed) {
            throw new IllegalArgumentException("[" + listener + "] is not registered");
        }
    }

    public void start() throws Exception {
        registerEndPoints();

        sendLifecycleEventToCluster(LifecycleState.STARTING);
        try {
            dispatcher.start();
            sendLifecycleEventToCluster(LifecycleState.STARTED);
        } catch (Exception e) {
            sendLifecycleEventToCluster(LifecycleState.FAILED);
            unregisterEndPoints();
            throw e;
        }

        serviceRegistry.start();
        
        registerServiceSpace();
    }

    public void stop() throws Exception {
        unregisterServiceSpace();
        
        synchronized (hostingPeers) {
            hostingPeers.clear();
        }
        
        try {
            serviceRegistry.stop();
        } catch (Exception e) {
            log.warn("Error while stopping service registry", e);
        }

        sendLifecycleEventToCluster(LifecycleState.STOPPING);
        synchronized (monitors) {
            for (ServiceMonitor monitor : monitors) {
                if (monitor.isStarted()) {
                    try {
                        monitor.stop();
                    } catch (Exception e) {
                        log.warn("Exception while stopping [" + monitor + "]", e);
                    }
                }
            }
        }
        try {
            dispatcher.stop();
            sendLifecycleEventToCluster(LifecycleState.STOPPED);
        } catch (Exception e) {
            sendLifecycleEventToCluster(LifecycleState.FAILED);
            log.warn("Exception while stopping [" + dispatcher + "]", e);
        }

        unregisterEndPoints();
    }
    
    public ServiceProxyFactory getServiceProxyFactory(ServiceName serviceName, Class[] interfaces) {
        return new CGLIBServiceProxyFactory(serviceName, interfaces, new BasicServiceInvoker(this, serviceName));
    }

    public Dispatcher getUnderlyingDispatcher() {
        return underlyingDispatcher;
    }

    protected void registerServiceSpace() {
        ServiceSpaceRegistry registry = getServiceSpaceRegistry();
        registry.register(this);
    }

    protected void unregisterServiceSpace() {
        ServiceSpaceRegistry registry = getServiceSpaceRegistry();
        registry.unregister(this);
    }
    
    protected ServiceSpaceRegistry getServiceSpaceRegistry() {
        ServiceSpaceRegistryFactory registryFactory = newServiceSpaceRegistryFactory();
        return registryFactory.getRegistryFor(underlyingDispatcher);
    }

    protected ServiceSpaceRegistryFactory newServiceSpaceRegistryFactory() {
        return new ServiceSpaceRegistryFactory();
    }

    protected StartableServiceRegistry newServiceRegistry() {
        return new BasicServiceRegistry(this);
    }

    protected Dispatcher newDispatcher() {
        return new BasicServiceSpaceDispatcher(this);
    }
    
    protected ServiceMonitor newServiceMonitor(ServiceName serviceName) {
        return new BasicServiceMonitor(this, serviceName);
    }

    protected void registerEndPoints() {
        dispatcher.register(serviceSpaceRVEndPoint);
        dispatcher.register(lifecycleEndpoint);
        underlyingDispatcher.getCluster().addClusterListener(underlyingClusterListener);
        underlyingDispatcher.register(serviceSpaceEndpoint);
    }

    protected void unregisterEndPoints() {
        underlyingDispatcher.unregister(serviceSpaceEndpoint, 10, 500);
        underlyingDispatcher.getCluster().removeClusterListener(underlyingClusterListener);
        dispatcher.unregister(lifecycleEndpoint, 10, 500);
        dispatcher.unregister(serviceSpaceRVEndPoint, 10, 500);
    }
    
    protected void sendLifecycleEventToCluster(LifecycleState state) {
        ServiceSpaceLifecycleEvent event = new ServiceSpaceLifecycleEvent(name, localPeer, state);
        try {
            Envelope message = underlyingDispatcher.createEnvelope();
            envelopeHelper.setServiceSpaceName(message);
            message.setReplyTo(underlyingDispatcher.getCluster().getLocalPeer().getAddress());
            Address clusterAddress = underlyingDispatcher.getCluster().getAddress();
            message.setAddress(clusterAddress);
            message.setPayload(event);
            underlyingDispatcher.send(clusterAddress, message);
        } catch (MessageExchangeException e) {
            log.warn("Cannot send lifecycle event [" + event + "] to cluster", e);
        }
    }

    protected void notifyListeners(ServiceSpaceLifecycleEvent event, Set<Peer> newHostingPeers) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            ServiceSpaceListener listener = (ServiceSpaceListener) iter.next();
            listener.receive(event, newHostingPeers);
        }
    }
    
    protected void processLifecycleEvent(ServiceSpaceLifecycleEvent event) {
        log.debug("Processing event [" + event + "]");
        
        Peer hostingPeer = event.getHostingPeer();
        if (hostingPeer.equals(localPeer)) {
            return;
        }
        
        LifecycleState state = event.getState();
        Set<Peer> copyHostingPeers;
        synchronized (hostingPeers) {
            if (state == LifecycleState.STARTING) {
                try {
                    dispatcher.send(hostingPeer.getAddress(), 
                            new ServiceSpaceLifecycleEvent(name, localPeer, LifecycleState.AVAILABLE));
                } catch (MessageExchangeException e) {
                    log.error(e);
                }
            } else if (state == LifecycleState.STARTED || state == LifecycleState.AVAILABLE) {
                hostingPeers.add(hostingPeer);
            } else if (state == LifecycleState.STOPPING || state == LifecycleState.FAILED) {
                hostingPeers.remove(hostingPeer);
            }
            copyHostingPeers = new HashSet<Peer>(hostingPeers);
        }
        notifyListeners(event, copyHostingPeers);
    }
    
    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof BasicServiceSpace)) {
            return false;
        }
        BasicServiceSpace other = (BasicServiceSpace) obj;
        return name.equals(other.name);
    }
    
    public String toString() {
        return "ServiceSpace [" + name + "]";
    }

    protected class ServiceSpaceLifecycleEndpoint implements ServiceEndpoint {

        public void dispatch(Envelope envelope) throws Exception {
            ServiceSpaceLifecycleEvent event = (ServiceSpaceLifecycleEvent) envelope.getPayload();
            processLifecycleEvent(event);
        }

        public void dispose(int nbAttemp, long delayMillis) {
            return;
        }

        public boolean testDispatchEnvelope(Envelope envelope) {
            Serializable payload = envelope.getPayload();
            if (!(payload instanceof ServiceSpaceLifecycleEvent)) {
                return false;
            }
            ServiceSpaceLifecycleEvent event = (ServiceSpaceLifecycleEvent) payload;
            return event.getServiceSpaceName().equals(name);
        }
        
    }

    public class UnderlyingClusterListener implements ClusterListener {

        public void onListenerRegistration(Cluster cluster, Set existing) {
        }

        public void onMembershipChanged(Cluster cluster, Set joiners, Set leavers) {
            for (Iterator iter = leavers.iterator(); iter.hasNext();) {
                Peer leaver = (Peer) iter.next();
                boolean leaverIsHostingPeer;
                synchronized (hostingPeers) {
                    leaverIsHostingPeer = hostingPeers.contains(leaver);
                }
                if (leaverIsHostingPeer) {
                    processLifecycleEvent(new ServiceSpaceLifecycleEvent(name, leaver, LifecycleState.FAILED));
                }
            }
        }

    }
    
}
