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

import java.util.Map;
import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.group.MessageExchangeException;
import org.codehaus.wadi.group.impl.AbstractDispatcher;

/**
 * 
 * @version $Revision: 1603 $
 */
public class VMDispatcher extends AbstractDispatcher {
    private final VMLocalPeer localNode;
    private final VMLocalCluster cluster;
    
    public VMDispatcher(VMCluster cluster, String nodeName, long inactiveTime) {
        super(inactiveTime);
        
        localNode = new VMLocalPeer(nodeName);
        this.cluster = new VMLocalCluster(cluster, localNode);
    }
    
    public VMDispatcher(String clusterName, String peerName, long inactiveTime) {
        super(inactiveTime);
        localNode = new VMLocalPeer(peerName);
        cluster = new VMLocalCluster(VMCluster.ensureCluster(clusterName), localNode);
    }
    
    public Cluster getCluster() {
        return cluster;
    }

    public void setDistributedState(Map state) throws MessageExchangeException {
        cluster.setDistributedState(localNode, state);
    }

    public void start() throws MessageExchangeException {
        cluster.registerDispatcher(this);
    }

    public void stop() throws MessageExchangeException {
        cluster.unregisterDispatcher(this);
    }

    public String getPeerName(Address address) {
        if (null == address) {
            return "<NULL Destination>";
        }
        
        if (address instanceof VMAddress) {
            return ((VMAddress) address).getNodeName();
        } else if (address instanceof VMClusterAddress) {
            return ((VMClusterAddress) address).getClusterName();
        }
        
        throw new IllegalArgumentException("Expected " +
            VMAddress.class.getName() +
            " or " + VMClusterAddress.class.getName() +    
            ". Was:" + address.getClass().getName());
    }

    public void send(Address target, Message message) throws MessageExchangeException {
        cluster.send(target, message);
    }

    public Message createMessage() {
        return new VMMessage();
    }

    public Address getAddress(String name) {
        return cluster.getAddress(name);
    }
    
}
