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
package org.codehaus.wadi.group.vm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.Envelope;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.AbstractCluster;

/**
 * 
 * @version $Revision: 1603 $
 */
public class VMLocalCluster implements Cluster {
    private final VMBroker delegate;
    private final LocalPeer node;
    private final VMDispatcher dispatcher;
    private boolean running;

    public VMLocalCluster(VMBroker delegate, LocalPeer node, VMDispatcher dispatcher) {
        this.delegate = delegate;
        this.node = node;
        this.dispatcher = dispatcher;
    }
    
    public String getClusterName() {
        return delegate.getName();
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }
    
    public String getName() {
        return delegate.getName();
    }

    void registerDispatcher(VMDispatcher dispatcher) {
        running = true;
        delegate.registerDispatcher(dispatcher);
    }

    void unregisterDispatcher(VMDispatcher dispatcher) {
        delegate.unregisterDispatcher(dispatcher);
        running = false;
    }

    void send(Address to, Envelope message) throws MessageExchangeException {
        message.setAddress(to);
        AbstractCluster.clusterThreadLocal.set(this);
        delegate.send(to, message);
    }

    Address getAddress(String name) {
        return delegate.getAddress(name);
    }

    public int getPeerCount() {
        return delegate.getPeerCount();
    }

    public Address getAddress() {
        return delegate.getAddress();
    }

    public Peer getPeerFromAddress(Address address) {
        throw new UnsupportedOperationException("NYI");
    }

    public void addClusterListener(ClusterListener listener) {
        delegate.addClusterListener(new VMLocalClusterListener(this, listener, node));
    }

    public void removeClusterListener(ClusterListener listener) {
        delegate.removeClusterListener(new VMLocalClusterListener(this, listener, node));
    }

    public Map<Address, Peer> getRemotePeers() {
        Map peers = delegate.getPeers();
        peers.remove(node.getName());
        Map<Address, Peer> remotePeers=new HashMap<Address, Peer>(peers.size());
        for (Iterator i=peers.values().iterator(); i.hasNext();) {
            VMLocalPeer peer=(VMLocalPeer)i.next();
            remotePeers.put(peer.getAddress(), peer);
        }
        return remotePeers;
    }

    public void start() throws ClusterException {
    }

    public void stop() throws ClusterException {
    }

    public boolean waitOnMembershipCount(int membershipCount, long timeout) throws InterruptedException {
        long expired = 0;
        membershipCount--; // remove ourselves from the equation...
        while (getRemotePeers().size() != membershipCount && expired<timeout) {
            Thread.sleep(1000);
            expired += 1000;
        }
        return getRemotePeers().size() == membershipCount;
    }
    
    public LocalPeer getLocalPeer() {
        return node;
    }
    
    boolean isRunning() {
        return running;
    }
    
    public String toString() {
        return "VMLocalCluster for node " + node;
    }
}
