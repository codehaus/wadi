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
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.Lifecycle;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceMonitor;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceRegistry;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceSpaceListener;
import org.codehaus.wadi.servicespace.ServiceSpaceName;

/**
 * 
 * @version $Revision: $
 */
public class BasicServiceSpace implements ServiceSpace, Lifecycle {
    private static final Log log = LogFactory.getLog(BasicServiceSpace.class);
    
    private final Collection listeners;
    private final Set hostingPeers;
    private final Collection monitors;
    private final LocalPeer localPeer;
    private final StartableServiceRegistry serviceRegistry;
    private final ServiceSpaceName name;
    private final Dispatcher underlyingDispatcher;
    private final Dispatcher dispatcher;
    private final ServiceEndpoint serviceSpaceEndpoint;
    private final ServiceEndpoint lifecycleEndpoint;
    private final ClusterListener underlyingClusterListener;
    private final ServiceSpaceMessageHelper messageHelper;
    
    public BasicServiceSpace(ServiceSpaceName name, Dispatcher underlyingDispatcher) {
        if (null == name) {
            throw new IllegalArgumentException("name is required");
        } else if (null == underlyingDispatcher) {
            throw new IllegalArgumentException("underlyingDispatcher is required");
        }
        monitors = new ArrayList();
        hostingPeers = new HashSet();
        listeners = new ArrayList();

        this.name = name;
        this.underlyingDispatcher = underlyingDispatcher;
        this.dispatcher = newDispatcher();

        localPeer = dispatcher.getCluster().getLocalPeer();
        serviceRegistry = newServiceRegistry();
        serviceSpaceEndpoint = new ServiceSpaceEndpoing(this);
        lifecycleEndpoint = new ServiceSpaceLifecycleEndpoint();
        underlyingClusterListener = new UnderlyingClusterListener();
        messageHelper = new ServiceSpaceMessageHelper(this);
    }
    
    public void addServiceSpaceListener(ServiceSpaceListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
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
        boolean removed;
        synchronized (listeners) {
            removed = listeners.remove(listener);
        }
        if (!removed) {
            throw new IllegalArgumentException("[" + listener + "] is not registered");
        }
    }

    public synchronized void start() throws Exception {
        registerEndPoints();

        multicastLifecycleEvent(LifecycleState.STARTING);
        try {
            dispatcher.start();
            multicastLifecycleEvent(LifecycleState.STARTED);
        } catch (Exception e) {
            multicastLifecycleEvent(LifecycleState.FAILED);
            unregisterEndPoints();
            throw e;
        }

        serviceRegistry.start();
    }

    public synchronized void stop() throws Exception {
        hostingPeers.clear();
        
        try {
            serviceRegistry.stop();
        } catch (Exception e) {
            log.warn("Error while stopping service registry", e);
        }

        multicastLifecycleEvent(LifecycleState.STOPPING);
        synchronized (monitors) {
            for (Iterator iter = monitors.iterator(); iter.hasNext();) {
                ServiceMonitor monitor = (ServiceMonitor) iter.next();
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
            multicastLifecycleEvent(LifecycleState.STOPPED);
        } catch (Exception e) {
            multicastLifecycleEvent(LifecycleState.FAILED);
            log.warn("Exception while stopping [" + dispatcher + "]", e);
        }

        unregisterEndPoints();
    }

    public Dispatcher getUnderlyingDispatcher() {
        return underlyingDispatcher;
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
        dispatcher.register(lifecycleEndpoint);
        underlyingDispatcher.getCluster().addClusterListener(underlyingClusterListener);
        underlyingDispatcher.register(serviceSpaceEndpoint);
    }

    protected void unregisterEndPoints() {
        underlyingDispatcher.unregister(serviceSpaceEndpoint, 10, 500);
        underlyingDispatcher.getCluster().removeClusterListener(underlyingClusterListener);
        dispatcher.unregister(lifecycleEndpoint, 10, 500);
    }
    
    protected void multicastLifecycleEvent(LifecycleState state) {
        ServiceSpaceLifecycleEvent event = new ServiceSpaceLifecycleEvent(name, localPeer, state);
        Map peerMulticasted = underlyingDispatcher.getCluster().getRemotePeers();
        for (Iterator iter = peerMulticasted.values().iterator(); iter.hasNext();) {
            Peer peer = (Peer) iter.next();
            try {
                Envelope message = underlyingDispatcher.createMessage();
                messageHelper.setServiceSpaceName(message);
                message.setReplyTo(underlyingDispatcher.getCluster().getLocalPeer().getAddress());
                message.setAddress(peer.getAddress());
                message.setPayload(event);
                underlyingDispatcher.send(peer.getAddress(), message);
            } catch (MessageExchangeException e) {
                log.warn("Cannot send lifecycle event [" + event + "] to [" + peer + "]. This peer is gone?", e);
            }
        }
    }

    protected void notifyListeners(ServiceSpaceLifecycleEvent event) {
        synchronized (listeners) {
            for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                ServiceSpaceListener listener = (ServiceSpaceListener) iter.next();
                listener.receive(event);
            }
        }        
    }
    
    protected void processLifecycleEvent(ServiceSpaceLifecycleEvent event) {
        log.info("Processing event [" + event + "]");
        
        Peer hostingPeer = event.getHostingPeer();
        LifecycleState state = event.getState();
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
            notifyListeners(event);
        }
    }
    
    protected class ServiceSpaceLifecycleEndpoint implements ServiceEndpoint {

        public void dispatch(Envelope om) throws Exception {
            ServiceSpaceLifecycleEvent event = (ServiceSpaceLifecycleEvent) om.getPayload();
            processLifecycleEvent(event);
        }

        public void dispose(int nbAttemp, long delayMillis) {
            return;
        }

        public boolean testDispatchMessage(Envelope om) {
            Serializable payload = om.getPayload();
            if (!(payload instanceof ServiceSpaceLifecycleEvent)) {
                return false;
            }
            ServiceSpaceLifecycleEvent event = (ServiceSpaceLifecycleEvent) payload;
            return event.getServiceSpaceName().equals(name);
        }
        
    }

    public class UnderlyingClusterListener implements ClusterListener {

        public void onListenerRegistration(Cluster cluster, Set existing, Peer coordinator) {
        }

        public void onMembershipChanged(Cluster cluster, Set joiners, Set leavers, Peer coordinator) {
            for (Iterator iter = leavers.iterator(); iter.hasNext();) {
                Peer leaver = (Peer) iter.next();
                synchronized (hostingPeers) {
                    if (hostingPeers.contains(leaver)) {
                        processLifecycleEvent(new ServiceSpaceLifecycleEvent(name, leaver, LifecycleState.FAILED));
                    }
                }
            }
        }

    }

}
