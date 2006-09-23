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
import org.codehaus.wadi.Lifecycle;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.ServiceEndpoint;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceListener;
import org.codehaus.wadi.servicespace.ServiceMonitor;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceSpaceListener;

/**
 * 
 * @version $Revision: $
 */
public class BasicServiceMonitor implements ServiceMonitor, Lifecycle {
    private static final Log log = LogFactory.getLog(BasicServiceMonitor.class);
    
    private final Dispatcher dispatcher;
    private final LocalPeer localPeer;
    private final ServiceSpace serviceSpace;
    private final ServiceName serviceName;
    private final Collection listeners;
    private final Set hostingPeers;
    private final ServiceLifecycleEndpoint lifecycleEndpoint;
    private final ServiceSpaceListener hostingServiceSpaceFailure;
    private boolean started;
    
    public BasicServiceMonitor(ServiceSpace serviceSpace, ServiceName serviceName) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == serviceName) {
            throw new IllegalArgumentException("serviceName is required");
        }
        this.serviceSpace = serviceSpace;
        this.serviceName = serviceName;

        dispatcher = serviceSpace.getDispatcher();
        localPeer = dispatcher.getCluster().getLocalPeer();
        listeners = new ArrayList();
        hostingPeers = new HashSet();
        lifecycleEndpoint = new ServiceLifecycleEndpoint();
        hostingServiceSpaceFailure = new HostingServiceSpaceFailure();
    }

    public void addServiceLifecycleListener(ServiceListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public Set getHostingPeers() {
        synchronized (hostingPeers) {
            return new HashSet(hostingPeers);
        }
    }

    public void removeServiceLifecycleListener(ServiceListener listener) {
        boolean removed;
        synchronized (listeners) {
            removed = listeners.remove(listener);
        }
        if (!removed) {
            throw new IllegalArgumentException("[" + listener + "] was not a registered listener");
        }
    }

    public synchronized boolean isStarted() {
        return started;
    }
    
    public synchronized void start() throws Exception {
        serviceSpace.addServiceSpaceListener(hostingServiceSpaceFailure);
        dispatcher.register(lifecycleEndpoint);
        started = true;
        
        ServiceQueryEvent event = new ServiceQueryEvent(serviceSpace.getServiceSpaceName(), serviceName, localPeer);
        Collection peers = dispatcher.getCluster().getRemotePeers().values();
        for (Iterator iter = peers.iterator(); iter.hasNext();) {
            Peer peer = (Peer) iter.next();
            try {
                dispatcher.send(peer.getAddress(), event);
            } catch (MessageExchangeException e) {
                log.warn("Cannot send lifecycle event [" + event + "] to [" + peer + "]. This peer is gone?", e);
            }
        }
    }
    
    public synchronized void stop() throws Exception {
        dispatcher.unregister(lifecycleEndpoint, 10, 500);
        serviceSpace.removeServiceSpaceListener(hostingServiceSpaceFailure);
    }
    
    protected void notifyListeners(ServiceLifecycleEvent event) {
        synchronized (listeners) {
            for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                ServiceListener listener = (ServiceListener) iter.next();
                listener.receive(event);
            }
        }        
    }

    protected void processLifecycleEvent(ServiceLifecycleEvent event) {
        log.info("Processing event [" + event + "]");
        
        LifecycleState state = event.getState();
        synchronized (hostingPeers) {
            if (state == LifecycleState.STARTED || state == LifecycleState.AVAILABLE) {
                hostingPeers.add(event.getHostingPeer());
            } else if (state == LifecycleState.STOPPED || state == LifecycleState.FAILED) {
                hostingPeers.remove(event.getHostingPeer());
            }
            notifyListeners(event);
        }
    }
    
    protected class ServiceLifecycleEndpoint implements ServiceEndpoint {

        public void dispatch(Message om) throws Exception {
            ServiceLifecycleEvent event = (ServiceLifecycleEvent) om.getPayload();
            processLifecycleEvent(event);
        }

        public void dispose(int nbAttemp, long delayMillis) {
            return;
        }

        public boolean testDispatchMessage(Message om) {
            Serializable payload = om.getPayload();
            if (!(payload instanceof ServiceLifecycleEvent)) {
                return false;
            }
            ServiceLifecycleEvent event = (ServiceLifecycleEvent) payload;
            return event.getServiceName().equals(serviceName);
        }
        
    }
    
    protected class HostingServiceSpaceFailure implements ServiceSpaceListener {

        public void receive(ServiceSpaceLifecycleEvent event) {
            if (event.getState() == LifecycleState.FAILED) {
                Peer failingPeer = event.getHostingPeer();
                synchronized (hostingPeers) {
                    boolean removed = hostingPeers.remove(failingPeer);
                    if (removed) {
                        notifyListeners(
                                new ServiceLifecycleEvent(serviceSpace.getServiceSpaceName(), 
                                        serviceName, 
                                        failingPeer, 
                                        LifecycleState.FAILED));
                    }
                }
            }
        }
        
    }
    
    public String toString() {
        return "ServiceMonitor for service[" + serviceName + "] in space [" + serviceSpace + "]";
    }

}
