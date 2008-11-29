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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceHolder;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceSpaceListener;
import org.codehaus.wadi.servicespace.SingletonService;
import org.codehaus.wadi.servicespace.SingletonServiceHolder;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicSingletonServiceHolder implements SingletonServiceHolder {
    private static final Log log = LogFactory.getLog(BasicSingletonServiceHolder.class);

    private final ServiceSpace serviceSpace;
    private final Object service;
    private final ServiceHolder delegate;
    private final Peer localPeer;
    private final ServiceSpaceListener singletonElector;
    private final Object hostingPeerLock = new Object();
    private Peer hostingPeer;

    public BasicSingletonServiceHolder(ServiceSpace serviceSpace, ServiceName serviceName, Object service) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == serviceName) {
            throw new IllegalArgumentException("serviceName is required");
        } else if (null == service) {
            throw new IllegalArgumentException("service is required");
        }
        this.serviceSpace = serviceSpace;
        this.service = service;

        delegate = newDelegateServiceHolder(serviceSpace, serviceName, service);
        localPeer = serviceSpace.getDispatcher().getCluster().getLocalPeer();
        singletonElector = newSingletonElector();
    }

    public void start() throws Exception {
        serviceSpace.addServiceSpaceListener(singletonElector);
        Set<Peer> hostingPeers = serviceSpace.getHostingPeers();
        elect(hostingPeers, false);
    }

    public void stop() throws Exception {
        synchronized (hostingPeerLock) {
            if (isLocal()) {
                onDismissal();
            }
            hostingPeer = null;
        }
        serviceSpace.removeServiceSpaceListener(singletonElector);
    }

    public boolean isLocal() {
        synchronized (hostingPeerLock) {
            return hostingPeer == localPeer;
        }
    }
    
    public Peer getHostingPeer() {
        synchronized (hostingPeerLock) {
            return hostingPeer;
        }
    }

    public Object getService() {
        return service;
    }

    public boolean isStarted() {
        return delegate.isStarted();
    }

    protected void updateHostingPeer(Peer newHostingPeer, boolean callbackService) {
        synchronized (hostingPeerLock) {
            if (newHostingPeer != hostingPeer) {
                if (isLocal()) {
                    onDismissal();
                } else if (newHostingPeer == localPeer) {
                    onElection();

                    if (callbackService && service instanceof SingletonService) {
                        ((SingletonService) service).onBecomeSingletonDueToMembershipUpdate();
                    }
                }
                hostingPeer = newHostingPeer;
            }
        }
    }

    protected void onElection() {
        log.info("[" + localPeer + "] owns singleton service [" + service + "]");
        try {
            delegate.start();
        } catch (Exception e) {
            log.error("Problem starting singleton service", e);
        }
    }

    protected void onDismissal() {
        log.info("[" + localPeer + "] resigns ownership of singleton service [" + service + "]");
        try {
            delegate.stop();
        } catch (Exception e) {
            log.error("Problem stopping singleton service", e);
        }
    }

    protected void elect(Set<Peer> newHostingPeers, boolean callbackService) {
        Peer oldest = localPeer;
        long oldestBirthtime = localPeer.getPeerInfo().getBirthtime();
        for (Peer hostingPeer : newHostingPeers) {
            long birthTime = hostingPeer.getPeerInfo().getBirthtime();
            if (oldestBirthtime > birthTime) {
                oldest = hostingPeer;
                oldestBirthtime = birthTime;
            }
        }
        updateHostingPeer(oldest, callbackService);
    }

    protected ServiceHolder newDelegateServiceHolder(ServiceSpace serviceSpace,
        ServiceName serviceName,
        Object service) {
        return new BasicServiceHolder(serviceSpace, serviceName, service);
    }

    protected SeniorityElector newSingletonElector() {
        return new SeniorityElector();
    }

    protected class SeniorityElector implements ServiceSpaceListener {

        public void receive(ServiceSpaceLifecycleEvent event, Set<Peer> newHostingPeers) {
            LifecycleState state = event.getState();
            if (state != LifecycleState.AVAILABLE && state != LifecycleState.FAILED &&
                    state != LifecycleState.STARTED && state != LifecycleState.STOPPED) {
                return;
            }
            elect(newHostingPeers, true);
        }

    }

}

