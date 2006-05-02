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
    private final String name;
    private final Address address;
    private final Map nodeNameToDispatcher = new HashMap();
    private final ClusterListenerSupport listenerSupport;
    private ElectionStrategy electionStrategy;
    private MessageRecorder messageRecorder;
    private Peer coordinator;
    
    public VMCluster(String name) {
        this.name = name;

        address = new VMClusterAddress(this);
        listenerSupport = new ClusterListenerSupport(this);
    }
    
    String getName() {
        return name;
    }

    void registerDispatcher(VMDispatcher dispatcher) {
        String nodeName = dispatcher.getLocalPeer().getName();
        synchronized (nodeNameToDispatcher) {
            nodeNameToDispatcher.put(nodeName, dispatcher);
        }
        
        listenerSupport.notifyAdd(dispatcher.getLocalPeer());
        doElection(dispatcher.getCluster());
    }

    void unregisterDispatcher(VMDispatcher dispatcher) {
        String nodeName = dispatcher.getLocalPeer().getName();
        Object object;
        synchronized (nodeNameToDispatcher) {
            object = nodeNameToDispatcher.remove(nodeName);
        }
        if (null == object) {
            throw new IllegalArgumentException("unknown dispatcher");
        }

        listenerSupport.notifyRemoved(dispatcher.getLocalPeer());
        doElection(dispatcher.getCluster());
    }

    void send(Address to, Message message) throws MessageExchangeException {
        if (null != messageRecorder) {
            messageRecorder.record(to, message);
        }
      
        if (to.equals(address)) {
            sendToClusterDestination(message);
        } else {
            sendToAddress(to, message);        
        }
    }

    int getNumNodes() {
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
                return dispatcher.getLocalAddress();
            }
        }

        throw new IllegalArgumentException("Node node having the name:" + name);
    }
    
    void setDistributedState(VMLocalPeer localNode, Map state) throws MessageExchangeException {
        localNode.setState(state);
        listenerSupport.notifyUpdate(localNode);
    }


    void setCoordinator(Peer coordinator) {
        this.coordinator = coordinator;
        listenerSupport.notifyCoordinatorChanged(coordinator);
    }

    public Address getAddress() {
        return address;
    }

    public Map getRemotePeers() {
        Map snapshotMap = snapshotDispatcherMap();

        for (Iterator iter = snapshotMap.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            Dispatcher dispatcher = (Dispatcher) entry.getValue();
            entry.setValue(dispatcher.getLocalPeer());  
        }
        return snapshotMap;
    }

    public void setElectionStrategy(ElectionStrategy electionStrategy) {
        throw new UnsupportedOperationException();
    }

    public void addClusterListener(ClusterListener listener) {
        listenerSupport.addClusterListener(listener);
    }

    public void removeClusterListener(ClusterListener listener) {
        listenerSupport.removeClusterListener(listener);
    }

    public boolean waitForClusterToComplete(int i, long timeout) throws InterruptedException {
        return true;
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
        
        listenerSupport.notifyFailed(dispatcher.getLocalPeer());
        doElection(dispatcher.getCluster());
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
            if (dispatcher.getLocalAddress().equals(to)) {
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
