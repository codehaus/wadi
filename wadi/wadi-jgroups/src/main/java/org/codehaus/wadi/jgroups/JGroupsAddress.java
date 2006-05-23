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
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1647 $
 */
public class JGroupsAddress implements Address {
	
	protected org.jgroups.Address _address;
	protected transient Peer _node; // backptr - initialsed later...
	
	protected JGroupsAddress(org.jgroups.Address address) {
		_address=address;
	}
	
	public void init(Peer node) {
		_node=node;
	}
	
	public org.jgroups.Address getAddress() {
		return _address;
	}
	
	public Peer getNode() {
		return _node;
	}
	
	public String getName() {
		return _node==null?"unknown":_node.getName();
	}
	
	protected static final String _prefix="<"+JGroupsAddress.class.getPackage().getName()+": ";
	protected static final String _suffix=">";
	
	public String toString() {
		String name=(_node==null?"cluster":_node.getName());
		return _prefix+name+_suffix;
	}
	
	public int hashCode() {
		return getAddress().hashCode();
	}
	
	public boolean equals(Object o) {
		if(this==o)
			return true;
		if(null==o || getClass()!=o.getClass())
			return false;
		
		return getAddress().equals(((JGroupsAddress)o).getAddress()); 
	}
	
	public int compareTo(Object o) {
		if (getClass()==o.getClass())
			return getAddress().compareTo(((JGroupsAddress)o).getAddress());
		else
			throw new IllegalArgumentException("expected object of type: "+getClass()+" but received Object of type: "+o.getClass());
	}
	
	
	protected Object readResolve() throws ObjectStreamException {
		// somehow always return same instance...
		Object tmp=get(_address);
		return tmp;
	}
	
	// temp hack until I figure out how best to do this...
	
	
	public static JGroupsAddress get(org.jgroups.Address address) {
		JGroupsCluster cluster=(JGroupsCluster)JGroupsCluster._cluster.get();
		return get(cluster, address);
	}
	
	public static JGroupsAddress get(JGroupsCluster cluster, org.jgroups.Address address) {
		if (address==null) {
            return (JGroupsAddress)cluster.getAddress();
        }
		
		// TODO - optimise locking here later
		JGroupsAddress destination;
		synchronized (cluster._addressToDestination) {
			destination=(JGroupsAddress)cluster._addressToDestination.get(address);
			if (destination==null) {
				destination=new JGroupsAddress(address);
				Peer node=new JGroupsRemotePeer(cluster, destination, cluster.getClusterState());
				destination.init(node);
				cluster._addressToDestination.put(address, destination);
			}
		}
		return destination;
	}
	
}
