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
import java.util.Iterator;
import java.util.Set;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Envelope;
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

import EDU.oswego.cs.dl.util.concurrent.Latch;

/**
 * 
 * @version $Revision: $
 */
public class BasicServiceSpaceDispatcher extends AbstractDispatcher {

    private final ServiceSpace serviceSpace;
    private final Dispatcher underlyingDispatcher;
    private final ServiceSpaceMessageHelper messageHelper;
    private final BasicServiceSpaceCluster serviceSpaceCluster;
    private final long waitTimeToBootServiceSpace;

    public BasicServiceSpaceDispatcher(BasicServiceSpace serviceSpace, long waitTimeToBootServiceSpace) {
        super(new ExecuteInThread());
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (0 > waitTimeToBootServiceSpace) {
            throw new IllegalArgumentException("waitTimeToBootServiceSpace must be greater than 0");
        }
        this.serviceSpace = serviceSpace;
        this.waitTimeToBootServiceSpace = waitTimeToBootServiceSpace;
        this.underlyingDispatcher = serviceSpace.getUnderlyingDispatcher();

        messageHelper = new ServiceSpaceMessageHelper(serviceSpace);
        serviceSpaceCluster = new BasicServiceSpaceCluster();
    }

    public Envelope createMessage() {
        Envelope message = underlyingDispatcher.createMessage();
        messageHelper.setServiceSpaceName(message);
        return message;
    }

    public Address getAddress(String name) {
        return underlyingDispatcher.getAddress(name);
    }

    public Cluster getCluster() {
        return serviceSpaceCluster;
    }

    public String getPeerName(Address address) {
        return underlyingDispatcher.getPeerName(address);
    }

    public void send(Address target, Envelope message) throws MessageExchangeException {
        underlyingDispatcher.send(target, message);
    }

    public synchronized void start() throws MessageExchangeException {
        try {
            serviceSpaceCluster.start();
        } catch (Exception e) {
            throw new ServiceSpaceException(e);
        }
    }

    public synchronized void stop() throws MessageExchangeException {
        try {
            serviceSpaceCluster.stop();
        } catch (Exception e) {
            throw new ServiceSpaceException(e);
        }
    }

    protected static class ExecuteInThread implements ThreadPool {

        public void execute(Runnable runnable) throws InterruptedException {
            runnable.run();
        }

    }

    protected class BasicServiceSpaceCluster extends AbstractCluster {
        private final ServiceSpaceListener listener;
        private Latch startLatch;
        
        public BasicServiceSpaceCluster() {
            super(underlyingDispatcher.getCluster().getClusterName() + "." + serviceSpace.getServiceSpaceName(),
                    underlyingDispatcher.getCluster().getLocalPeer().getName(),
                    BasicServiceSpaceDispatcher.this);
            
            startLatch = new Latch();
            listener = new ServiceSpaceListener() {

                public void receive(ServiceSpaceLifecycleEvent event, Set newHostingPeers) {
                    LifecycleState state = event.getState();
                    Peer hostingPeer = event.getHostingPeer();
                    Set joiners = Collections.EMPTY_SET;
                    Set leavers = Collections.EMPTY_SET;
                    synchronized (_addressToPeer) {
                        if (state == LifecycleState.STARTED || state == LifecycleState.AVAILABLE) {
                            _addressToPeer.put(hostingPeer.getAddress(), hostingPeer);
                            joiners = Collections.unmodifiableSet(Collections.singleton(hostingPeer));
                            notifyMembershipChanged(joiners, leavers);
                        } else if (state == LifecycleState.STOPPED || state == LifecycleState.FAILED) {
                            _addressToPeer.remove(hostingPeer.getAddress());
                            leavers = Collections.unmodifiableSet(Collections.singleton(hostingPeer));
                            notifyMembershipChanged(joiners, leavers);
                        }
                    }
                    if (state == LifecycleState.AVAILABLE) {
                        startLatch.release();
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
            Set hostingPeers = serviceSpace.getHostingPeers();
            synchronized (_addressToPeer) {
                for (Iterator iter = hostingPeers.iterator(); iter.hasNext();) {
                    Peer peer = (Peer) iter.next();
                    _addressToPeer.put(peer.getAddress(), peer);
                    startLatch.release();
                }
                if (!hostingPeers.isEmpty()) {
                    notifyMembershipChanged(hostingPeers, Collections.EMPTY_SET);
                }
            }
            
            boolean isFirstPeer;
            try {
                isFirstPeer = !startLatch.attempt(waitTimeToBootServiceSpace);
            } catch (InterruptedException e) {
                throw (IllegalStateException) new IllegalStateException().initCause(e);
            }
            if (isFirstPeer) {
                setFirstPeer();
            }
        }

        public synchronized void stop() throws ClusterException {
            serviceSpace.removeServiceSpaceListener(listener);
            startLatch = new Latch();
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
