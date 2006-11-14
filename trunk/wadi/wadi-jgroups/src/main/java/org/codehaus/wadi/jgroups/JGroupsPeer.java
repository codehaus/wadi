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
package org.codehaus.wadi.jgroups;

import java.io.ObjectStreamException;

import org.codehaus.wadi.group.Address;
import org.codehaus.wadi.group.EndPoint;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.group.PeerInfo;

public class JGroupsPeer implements Peer, Address, Comparable {

    protected final transient JGroupsCluster _cluster;
    protected final String name;
    private final PeerInfo peerInfo;
    protected org.jgroups.Address _jgAddress;

    public JGroupsPeer(JGroupsCluster cluster, String name, EndPoint endPoint) {
        if (null == cluster) {
            throw new IllegalArgumentException("cluster is required");
        } else if (null == name) {
            throw new IllegalArgumentException("name is required");
        }
        _cluster = cluster;
        this.name = name;

        peerInfo = new PeerInfo(endPoint);
    }

    public JGroupsPeer(JGroupsCluster cluster, JGroupsPeer prototype) {
        if (null == cluster) {
            throw new IllegalArgumentException("cluster is required");
        } else if (null == prototype) {
            throw new IllegalArgumentException("prototype is required");
        }
        _cluster = cluster;
        this.name = prototype.name;
        this.peerInfo = prototype.peerInfo;
    }

    protected Object readResolve() throws ObjectStreamException {
        return JGroupsCluster.get(this);
    }

    public int hashCode() {
        return _jgAddress == null ? 0 : _jgAddress.hashCode();
    }

    public boolean equals(Object object) {
        return this == object;
    }

    public int compareTo(Object object) {
        return _jgAddress.compareTo(((JGroupsPeer) object).getJGAddress());
    }

    public Address getAddress() {
        return this;
    }

    public String getName() {
        return name;
    }

    public void init(org.jgroups.Address jgAddress) {
        _jgAddress = jgAddress;
    }

    public org.jgroups.Address getJGAddress() {
        return _jgAddress;
    }

    public PeerInfo getPeerInfo() {
        return peerInfo; 
    }

}
