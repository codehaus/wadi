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

import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceName;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceSpaceListener;
import org.codehaus.wadi.servicespace.SingletonServiceHolder;

/**
 * 
 * @version $Revision: 1538 $
 */
public class BasicSingletonServiceHolder extends BasicServiceHolder implements SingletonServiceHolder {
    private static final Log log = LogFactory.getLog(BasicSingletonServiceHolder.class);
    
    private final Peer localPeer;
    private final ServiceSpaceListener singletonElector;
    private final Object hostingPeerLock = new Object();
    private Peer hostingPeer;


    public BasicSingletonServiceHolder(ServiceSpace serviceSpace, ServiceName serviceName, Lifecycle service) {
        super(serviceSpace, serviceName, service);
        
        localPeer = serviceSpace.getDispatcher().getCluster().getLocalPeer();
        singletonElector = newSingletonElector();
    }

    public void start() throws Exception {
        serviceSpace.addServiceSpaceListener(singletonElector);
        Set hostingPeers = serviceSpace.getHostingPeers();
        elect(hostingPeers);
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

    public Lifecycle getSingletonService() {
        return service;
    }

    protected void updateHostingPeer(Peer newHostingPeer) {
        synchronized (hostingPeerLock) {
            if (newHostingPeer != hostingPeer) {
                if (isLocal()) {
                    onDismissal();
                } else if (newHostingPeer == localPeer) {
                    onElection();
                }
                hostingPeer = newHostingPeer;
            }
        }
    }

    protected void onElection() {
        log.info("[" + localPeer + "] owns singleton service [" + service + "]");
        try {
            super.start();
        } catch (Exception e) {
            log.error("Problem starting singleton service", e);
        }
    }

    protected void onDismissal() {
        log.info("[" + localPeer + "] resigns ownership of singleton service [" + service + "]");
        try {
            super.stop();
        } catch (Exception e) {
            log.error("Problem stopping singleton service", e);
        }
    }

    protected void elect(Set newHostingPeers) {
        Peer oldest = localPeer;
        long oldestBirthtime = localPeer.getPeerInfo().getBirthtime();
        for (Iterator iter = newHostingPeers.iterator(); iter.hasNext();) {
            Peer hostingPeer = (Peer) iter.next();
            long birthTime = hostingPeer.getPeerInfo().getBirthtime();
            if (oldestBirthtime > birthTime) {
                oldest = hostingPeer;
                oldestBirthtime = birthTime;
            }
        }
        updateHostingPeer(oldest);
    }

    protected SeniorityElector newSingletonElector() {
        return new SeniorityElector();
    }

    protected class SeniorityElector implements ServiceSpaceListener {

        public void receive(ServiceSpaceLifecycleEvent event, Set newHostingPeers) {
            LifecycleState state = event.getState();
            if (state != LifecycleState.AVAILABLE && state != LifecycleState.FAILED &&
                    state != LifecycleState.STARTED && state != LifecycleState.STOPPED) {
                return;
            }
            elect(newHostingPeers);
        }

    }

}

