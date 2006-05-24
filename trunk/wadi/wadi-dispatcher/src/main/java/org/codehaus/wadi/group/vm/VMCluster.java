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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.ElectionStrategy;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;

/**
 * 
 * @version $Revision: 1603 $
 */
public class VMCluster implements Cluster {
    protected static final Map clusters=new HashMap();
    
    public static VMCluster ensureCluster(String clusterName) {
        VMCluster cluster = (VMCluster) clusters.get(clusterName);
        if (null == cluster) {
            cluster = new VMCluster(clusterName);
            clusters.put(clusterName, cluster);
        }
        return cluster;
    }
    
    private final String name;
    private final Address address;
    private final Map nodeNameToDispatcher = new HashMap();
    private final ClusterListenerSupport listenerSupport;
    private ElectionStrategy electionStrategy;
    private MessageRecorder messageRecorder;
    private MessageTransformer messageTransformer;
    private Peer coordinator;
    
    public VMCluster(String name) {
        this.name = name;

        address = new VMClusterAddress(this);
        listenerSupport = new ClusterListenerSupport(this);
        messageTransformer = new SerializeMessageTransformer(this);
    }

    public VMCluster(String name, boolean serializeMessages) {
        this.name = name;

        address = new VMClusterAddress(this);
        listenerSupport = new ClusterListenerSupport(this);
        if (serializeMessages) {
            messageTransformer = new SerializeMessageTransformer(this);
        } else {
            messageTransformer = new NoOpMessageTransformer();
        }
    }

    String getName() {
        return name;
    }

    void registerDispatcher(VMDispatcher dispatcher) {
        String nodeName = dispatcher.getCluster().getLocalPeer().getName();
        synchronized (nodeNameToDispatcher) {
            nodeNameToDispatcher.put(nodeName, dispatcher);
        }
        
        listenerSupport.notifyMembershipChanged(Collections.singleton(dispatcher.getCluster().getLocalPeer()), Collections.EMPTY_SET);
        doElection(dispatcher.getCluster());
    }

    void unregisterDispatcher(VMDispatcher dispatcher) {
        String nodeName = dispatcher.getCluster().getLocalPeer().getName();
        Object object;
        synchronized (nodeNameToDispatcher) {
            object = nodeNameToDispatcher.remove(nodeName);
        }
        if (null == object) {
            throw new IllegalArgumentException("unknown dispatcher");
        }

        // N.B. - this probably should not do anything now... - FIXME
        doElection(dispatcher.getCluster());
        listenerSupport.notifyMembershipChanged(Collections.singleton(dispatcher.getCluster().getLocalPeer()), Collections.EMPTY_SET);
    }

    void send(Address to, Message message) throws MessageExchangeException {
        if (null != messageRecorder) {
            messageRecorder.record(to, message);
        }
      
        message = messageTransformer.transform(message);
        
        if (to.equals(address)) {
            sendToClusterDestination(message);
        } else {
            sendToAddress(to, message);        
        }
    }

    public int getPeerCount() {
        synchronized (nodeNameToDispatcher) {
            return nodeNameToDispatcher.size();
        }
    }

    Address getAddress(String name) {
        Map snapshotMap = snapshotDispatcherMap();

        for (Iterator iter = snapshotMap.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            if (entry.getKey().equals(name)) {
                Dispatcher dispatcher = (Dispatcher) entry.getValue();
                return dispatcher.getCluster().getLocalPeer().getAddress();
            }
        }

        throw new IllegalArgumentException("Node node having the name:" + name);
    }
    
    void setDistributedState(VMLocalPeer localNode, Map state) throws MessageExchangeException {
        localNode.setState(state);
        listenerSupport.notifyUpdated(localNode);
    }


    void setCoordinator(Peer coordinator) {
        this.coordinator = coordinator;
        listenerSupport.notifyCoordinatorChanged(coordinator);
    }

    Map getPeers() {
        Map snapshotMap = snapshotDispatcherMap();

        for (Iterator iter = snapshotMap.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            Dispatcher dispatcher = (Dispatcher) entry.getValue();
            entry.setValue(dispatcher.getCluster().getLocalPeer());  
        }
        return snapshotMap;
    }

    public Address getAddress() {
        return address;
    }

    public Map getRemotePeers() {
        throw new UnsupportedOperationException();
    }

    public void setElectionStrategy(ElectionStrategy electionStrategy) {
        throw new UnsupportedOperationException();
    }

    public void addClusterListener(ClusterListener listener) {
        listenerSupport.addClusterListener((VMLocalClusterListener) listener);
    }

    public void removeClusterListener(ClusterListener listener) {
        listenerSupport.removeClusterListener((VMLocalClusterListener) listener);
    }

    public boolean waitOnMembershipCount(int membershipCount, long timeout) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    public LocalPeer getLocalPeer() {
        throw new UnsupportedOperationException();
    }
    
    public void start() throws ClusterException {
    }

    public void stop() throws ClusterException {
        synchronized (nodeNameToDispatcher) {
            nodeNameToDispatcher.clear();
        }
    }

    public void failNode(String nodeName) {
        VMDispatcher dispatcher;
        synchronized (nodeNameToDispatcher) {
            dispatcher = (VMDispatcher) nodeNameToDispatcher.remove(nodeName);
        }
        if (null == dispatcher) {
            throw new IllegalArgumentException("Node " + nodeName + " is unknown.");  
        }
        
        doElection(dispatcher.getCluster());
        listenerSupport.notifyMembershipChanged(Collections.EMPTY_SET, Collections.singleton(getLocalPeer()));
    }

    public void setMessageRecorder(MessageRecorder messageRecorder) {
        messageRecorder.setVMCluster(this);
        this.messageRecorder = messageRecorder;
    }
    
    private void doElection(Cluster cluster) {
        if (null != electionStrategy) {
            Peer newElected = electionStrategy.doElection(cluster);
            if (null != newElected && !newElected.equals(coordinator)) {
                coordinator = newElected;
                listenerSupport.notifyCoordinatorChanged(newElected);
            }
        }
    }

    private Map snapshotDispatcherMap() {
        synchronized (nodeNameToDispatcher) {
            return new HashMap(nodeNameToDispatcher);
        }
    }

    private void sendToAddress(Address to, Message message) {
        Map snapshotMap = snapshotDispatcherMap();
        
        for (Iterator iter = snapshotMap.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            Dispatcher dispatcher = (Dispatcher) entry.getValue();
            if (dispatcher.getCluster().getLocalPeer().getAddress().equals(to)) {
                dispatcher.onMessage(message);
                return;
            }
        }
        throw new IllegalArgumentException("Destination " + to + " is unknown.");
    }

    private void sendToClusterDestination(Message message) {
        Map snapshotMap = snapshotDispatcherMap();
        
        for (Iterator iter = snapshotMap.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            Dispatcher dispatcher = (Dispatcher) entry.getValue();
            dispatcher.onMessage(message);
        }
    }
}
