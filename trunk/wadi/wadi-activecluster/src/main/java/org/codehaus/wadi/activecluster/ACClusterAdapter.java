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
package org.codehaus.wadi.activecluster;

import java.util.Iterator;
import java.util.Map;

import javax.jms.JMSException;

import org.apache.activecluster.Node;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ClusterException;
import org.codehaus.wadi.group.ClusterListener;
import org.codehaus.wadi.group.ElectionStrategy;
import org.codehaus.wadi.group.LocalPeer;

/**
 * 
 * @version $Revision: 1603 $
 */
class ACClusterAdapter implements Cluster {
    private final org.apache.activecluster.Cluster adaptee;
    private final LocalPeer localPeer;
    private final Address address;
    
    public ACClusterAdapter(org.apache.activecluster.Cluster adaptee) {
        this.adaptee = adaptee;
        
        address = new ACDestinationAdapter(adaptee.getDestination());
        localPeer = new ACLocalNodeAdapter(adaptee.getLocalNode());
    }

    public Address getAddress() {
        return address;
    }
    
    public void setElectionStrategy(ElectionStrategy strategy) {
        adaptee.setElectionStrategy(new WADIElectionStrategyAdapter(strategy, this));
    }

    public Map getPeers() {
        Map nodes = adaptee.getNodes();
        for (Iterator iter = nodes.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            Node node = (Node) entry.getValue();
            entry.setValue(new ACNodeAdapter(node));
        }
        return nodes;
    }

    public void addClusterListener(ClusterListener listener) {
        adaptee.addClusterListener(new WADIClusterListenerAdapter(listener, this));
    }

    public void removeClusterListener(ClusterListener listener) {
        adaptee.removeClusterListener(new WADIClusterListenerAdapter(listener, this));
    }

    public LocalPeer getLocalPeer() {
        return localPeer;
    }
    
    public boolean waitForClusterToComplete(int i, long timeout) throws InterruptedException {
        return adaptee.waitForClusterToComplete(i, timeout);
    }

    public void start() throws ClusterException {
        try {
            adaptee.start();
        } catch (JMSException e) {
            throw new ClusterException(e);
        }
    }

    public void stop() throws ClusterException {
        try {
            adaptee.stop();
        } catch (JMSException e) {
            throw new ClusterException(e);
        }
    }
    
    public boolean equals(Object obj) {
        if (false == obj instanceof ACClusterAdapter) {
            return false;
        }
        
        ACClusterAdapter other = (ACClusterAdapter) obj;
        return adaptee.equals(other.adaptee);
    }
    
    public int hashCode() {
        return adaptee.hashCode();
    }
}