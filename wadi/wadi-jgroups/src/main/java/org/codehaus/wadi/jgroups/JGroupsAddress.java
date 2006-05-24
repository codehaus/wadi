/**
 *
 * Copyright 2003-2005 Core Developers Network Ltd.
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
import org.codehaus.wadi.group.Peer;

/**
 * A WADI Address mapped onto a JGroups Address.
 * 
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1647 $
 */
public class JGroupsAddress implements Address, Comparable {

    protected static final String _prefix="<"+JGroupsAddress.class.getPackage().getName()+": ";
    protected static final String _suffix=">";

    protected final transient Peer _peer;

    protected org.jgroups.Address _address;

    protected JGroupsAddress(Peer peer) {
        _peer=peer;
    }

    // 'java.lang.Object' API

    protected Object readResolve() throws ObjectStreamException {
        // somehow always return same instance...
        Object tmp=get(_address);
        return tmp;
    }

    public String toString() {
        String name=(_peer==null?"cluster":_peer.getName());
        return _prefix+name+_suffix;
    }

    public int hashCode() {
        return getAddress().hashCode();
    }

    public boolean equals(Object object) {
        return (this==object);
    }

    // 'java.lang.Comparable' API

    public int compareTo(Object o) {
        if (getClass()==o.getClass())
            return getAddress().compareTo(((JGroupsAddress)o).getAddress());
        else
            throw new IllegalArgumentException("expected object of type: "+getClass()+" but received Object of type: "+o.getClass());
    }

    // 'org.codehaus.wadi.jgroups.JGroupsAddress' API

    public void init(org.jgroups.Address address) {
        _address=address;
    }

    public Peer getPeer() {
        return _peer;
    }

    public org.jgroups.Address getAddress() {
        return _address;
    }

    public String getName() {
        return _peer==null?"unknown":_peer.getName();
    }

    // temp hack until I figure out how best to do this...

    public static JGroupsAddress get(org.jgroups.Address address) {
        JGroupsCluster cluster=(JGroupsCluster)JGroupsCluster._cluster.get();
        return get(cluster, address);
    }

    public static JGroupsAddress get(JGroupsCluster cluster, org.jgroups.Address jgaddress) {
        if (jgaddress==null) {
            return (JGroupsAddress)cluster.getAddress();
        }

        // TODO - optimise locking here later
        JGroupsAddress address;
        synchronized (cluster._jgAddressToAddress) {
            address=(JGroupsAddress)cluster._jgAddressToAddress.get(jgaddress);
            if (address==null) {
                Peer peer=new JGroupsRemotePeer(cluster, jgaddress, cluster.getClusterState());
                address=(JGroupsAddress)peer.getAddress();
                cluster._jgAddressToAddress.put(jgaddress, address);
            }
        }
        return address;
    }

}
