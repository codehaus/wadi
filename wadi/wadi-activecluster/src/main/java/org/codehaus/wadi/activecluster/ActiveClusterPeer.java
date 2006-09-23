/**
 *
 * Copyright 2003-2006 Core Developers Network Ltd.
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

import java.io.ObjectStreamException;

import javax.jms.Destination;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.PeerInfo;

public class ActiveClusterPeer implements Peer, Address, Comparable {

    protected final transient ActiveClusterCluster _cluster;
    protected String name;
    protected PeerInfo peerInfo;
    protected Destination _acDestination;

    public ActiveClusterPeer(ActiveClusterCluster cluster, String name) {
        if (null == cluster) {
            throw new IllegalArgumentException("cluster is required");
        } else if (null == name) {
            throw new IllegalArgumentException("name is required");
        }
        _cluster = cluster;
        this.name = name;
        
        peerInfo = new PeerInfo();
    }

    protected ActiveClusterPeer(ActiveClusterCluster cluster, ActiveClusterPeer prototype) {
        if (null == cluster) {
            throw new IllegalArgumentException("cluster is required");
        } else if (null == prototype) {
            throw new IllegalArgumentException("prototype is required");
        }
        _cluster = cluster;
        this.name = prototype.name;
        this.peerInfo = prototype.peerInfo;
        this._acDestination = prototype._acDestination;
    }

    protected Object readResolve() throws ObjectStreamException {
        // somehow always return same instance...
        return ActiveClusterCluster.get(this);
    }

    public int hashCode() {
        return _acDestination == null ? 0 : _acDestination.hashCode();
    }

    public boolean equals(Object object) {
        return this == object;
    }

    public int compareTo(Object object) {
        return _acDestination.toString().compareTo(((ActiveClusterPeer) object).getACDestination().toString());
    }

    public Address getAddress() {
        return this;
    }

    public String getName() {
        return name;
    }

    public PeerInfo getPeerInfo() {
        return peerInfo;
    }

    public void init(Destination acDestination) {
        _acDestination = acDestination;
    }

    public Destination getACDestination() {
        return _acDestination;
    }

}
