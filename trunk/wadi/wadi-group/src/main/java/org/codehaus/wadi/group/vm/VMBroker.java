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
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.Dispatcher;
import org.codehaus.wadi.group.ElectionStrategy;
import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.impl.SeniorityElectionStrategy;

/**
 *
 * @version $Revision: 1603 $
 */
public class VMBroker {
    protected final long inactiveTime=5000; // TODO - parameterise

    private final String name;
    private final Address address;
    private final Map nodeNameToDispatcher = new HashMap();
    private final ClusterListenerSupport listenerSupport;
    private ElectionStrategy electionStrategy=new SeniorityElectionStrategy();
    private MessageRecorder messageRecorder;
    private MessageTransformer messageTransformer;
    private Peer coordinator;

    public VMBroker(String name) {
        this.name = name;

        address = new VMClusterAddress(this);
        listenerSupport = new ClusterListenerSupport(this);
        messageTransformer = new SerializeMessageTransformer(this);
    }

    public VMBroker(String name, boolean serializeMessages) {
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
        LocalPeer localPeer = dispatcher.getCluster().getLocalPeer();
        String nodeName = localPeer.getName();
        synchronized (nodeNameToDispatcher) {
            nodeNameToDispatcher.put(nodeName, dispatcher);
        }

        electOnEachNode();

        // notify new peer of existing members...
        listenerSupport.notifyMembershipChanged(localPeer, true, coordinator);
    }

    void unregisterDispatcher(VMDispatcher dispatcher) {
        LocalPeer localPeer = dispatcher.getCluster().getLocalPeer();
        String nodeName = localPeer.getName();
        Object object;
        synchronized (nodeNameToDispatcher) {
            object = nodeNameToDispatcher.remove(nodeName);
        }
        if (null == object) {
            throw new IllegalArgumentException("unknown dispatcher");
        }

        electOnEachNode();

        listenerSupport.notifyMembershipChanged(localPeer, false, coordinator);
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

    int getPeerCount() {
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

    void setCoordinator(Peer coordinator) {
        this.coordinator = coordinator;
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

    Address getAddress() {
        return address;
    }

    void addClusterListener(VMLocalClusterListener listener) {
        listenerSupport.addClusterListener(listener, coordinator);
    }

    void removeClusterListener(VMLocalClusterListener listener) {
        listenerSupport.removeClusterListener(listener);
    }

    long getInactiveTime() {
        return inactiveTime;
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
    }

    public void setMessageRecorder(MessageRecorder messageRecorder) {
        messageRecorder.setVMCluster(this);
        this.messageRecorder = messageRecorder;
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

    private void electOnEachNode() {
        synchronized (nodeNameToDispatcher) {
            for (Iterator iter = nodeNameToDispatcher.values().iterator(); iter.hasNext();) {
                VMDispatcher remainingDispatcher = (VMDispatcher) iter.next();
                electCoordinator(remainingDispatcher);
            }
        }
    }

    private void electCoordinator(VMDispatcher dispatcher) {
        if (null != electionStrategy) {
            coordinator = electionStrategy.doElection(dispatcher.getCluster());
        }
    }
}
