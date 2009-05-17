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
import java.util.concurrent.CountDownLatch;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.AbstractCluster;
import org.codehaus.wadi.group.impl.AbstractDispatcher;
import org.codehaus.wadi.group.impl.ThreadPool;
import org.codehaus.wadi.servicespace.LifecycleState;
import org.codehaus.wadi.servicespace.ServiceSpace;
import org.codehaus.wadi.servicespace.ServiceSpaceException;
import org.codehaus.wadi.servicespace.ServiceSpaceLifecycleEvent;
import org.codehaus.wadi.servicespace.ServiceSpaceListener;

/**
 * 
 * @version $Revision: $
 */
public class BasicServiceSpaceDispatcher extends AbstractDispatcher {

    private final ServiceSpace serviceSpace;
    private final Dispatcher underlyingDispatcher;
    private final ServiceSpaceEnvelopeHelper envelopeHelper;
    private final BasicServiceSpaceCluster serviceSpaceCluster;

    public BasicServiceSpaceDispatcher(BasicServiceSpace serviceSpace, ServiceSpaceEnvelopeHelper envelopeHelper) {
        super(new ExecuteInThread());
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (null == envelopeHelper) {
            throw new IllegalArgumentException("envelopeHelper is required");
        }
        this.serviceSpace = serviceSpace;
        this.envelopeHelper = envelopeHelper;

        underlyingDispatcher = serviceSpace.getUnderlyingDispatcher();

        serviceSpaceCluster = new BasicServiceSpaceCluster();
    }

    public Envelope createEnvelope() {
        Envelope message = underlyingDispatcher.createEnvelope();
        envelopeHelper.setServiceSpaceName(message);
        return message;
    }

    public Cluster getCluster() {
        return serviceSpaceCluster;
    }

    public String getPeerName(Address address) {
        return underlyingDispatcher.getPeerName(address);
    }

    protected void doSend(Address target, Envelope envelope) throws MessageExchangeException {
        underlyingDispatcher.send(target, envelope);
    }

    public synchronized void start() throws MessageExchangeException {
        try {
            serviceSpaceCluster.start();
        } catch (Exception e) {
            throw new ServiceSpaceException(serviceSpace.getServiceSpaceName(), e);
        }
    }

    public synchronized void stop() throws MessageExchangeException {
        try {
            serviceSpaceCluster.stop();
        } catch (Exception e) {
            throw new ServiceSpaceException(serviceSpace.getServiceSpaceName(), e);
        }
    }

    protected static class ExecuteInThread implements ThreadPool {

        public void execute(Runnable runnable) throws InterruptedException {
            runnable.run();
        }

    }

    protected class BasicServiceSpaceCluster extends AbstractCluster {
        private final ServiceSpaceListener listener;
        private CountDownLatch startLatch;
        
        public BasicServiceSpaceCluster() {
            super(underlyingDispatcher.getCluster().getClusterName() + "." + serviceSpace.getServiceSpaceName(),
                    underlyingDispatcher.getCluster().getLocalPeer().getName(),
                    BasicServiceSpaceDispatcher.this);
            
            startLatch = new CountDownLatch(1);
            listener = new ServiceSpaceListener() {

                public void receive(ServiceSpaceLifecycleEvent event, Set<Peer> newHostingPeers) {
                    LifecycleState state = event.getState();
                    Peer hostingPeer = event.getHostingPeer();
                    Set<Peer> joiners = Collections.emptySet();
                    Set<Peer> leavers = Collections.emptySet();
                    synchronized (addressToPeer) {
                        if (state == LifecycleState.STARTED || state == LifecycleState.AVAILABLE) {
                            addressToPeer.put(hostingPeer.getAddress(), hostingPeer);
                            joiners = Collections.unmodifiableSet(Collections.singleton(hostingPeer));
                            notifyMembershipChanged(joiners, leavers);
                        } else if (state == LifecycleState.STOPPED || state == LifecycleState.FAILED) {
                            addressToPeer.remove(hostingPeer.getAddress());
                            leavers = Collections.unmodifiableSet(Collections.singleton(hostingPeer));
                            notifyMembershipChanged(joiners, leavers);
                        }
                    }
                    if (state == LifecycleState.AVAILABLE) {
                        startLatch.countDown();
                    }
                }

            };
        }

        protected Object extractKeyFromPeerSerialization(Object backend) {
            throw new UnsupportedOperationException();
        }
        
        protected Peer createPeerFromPeerSerialization(Object backend) {
            throw new UnsupportedOperationException();
        }

        public synchronized void start() throws ClusterException {
            serviceSpace.addServiceSpaceListener(listener);
            Set<Peer> hostingPeers = serviceSpace.getHostingPeers();
            synchronized (addressToPeer) {
                for (Peer peer : hostingPeers) {
                    addressToPeer.put(peer.getAddress(), peer);
                }
                if (!hostingPeers.isEmpty()) {
                    notifyMembershipChanged(hostingPeers, Collections.EMPTY_SET);
                }
            }
        }

        public synchronized void stop() throws ClusterException {
            serviceSpace.removeServiceSpaceListener(listener);
            startLatch = new CountDownLatch(1);
        }

        public Address getAddress() {
            return underlyingDispatcher.getCluster().getAddress();
        }

        public LocalPeer getLocalPeer() {
            return underlyingDispatcher.getCluster().getLocalPeer();
        }

        public Peer getPeerFromAddress(Address address) {
            return underlyingDispatcher.getCluster().getPeerFromAddress(address);
        }

    }
    
}
