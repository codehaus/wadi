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
import java.io.Serializable;

import javax.jms.Destination;

import org.apache.activecluster.Node;
import org.jgroups.Address;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JGroupsDestination implements Destination, Serializable {
	
	protected Address _address;
	protected transient Node _node; // backptr - initialsed later...
	
	protected JGroupsDestination(Address address) {
		_address=address;
	}
	
	public void init(Node node) {
		_node=node;
	}
	
	public Address getAddress() {
		return _address;
	}
	
	public Node getNode() {
		return _node;
	}
	
	public String getName() {
		return _node==null?"unknown":_node.getName();
	}
	
	protected static final String _prefix="<"+JGroupsDestination.class.getPackage().getName()+": ";
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
		
		return getAddress().equals(((JGroupsDestination)o).getAddress()); 
	}
	
	public int compareTo(Object o) {
		if (getClass()==o.getClass())
			return getAddress().compareTo(((JGroupsDestination)o).getAddress());
		else
			throw new IllegalArgumentException("expected object of type: "+getClass()+" but received Object of type: "+o.getClass());
	}
	
	
	protected Object readResolve() throws ObjectStreamException {
		// somehow always return same instance...
		Object tmp=get(_address);
		return tmp;
	}
	
	// temp hack until I figure out how best to do this...
	
	
	public static JGroupsDestination get(Address address) {
		JGroupsCluster cluster=(JGroupsCluster)JGroupsCluster._cluster.get();
		return get(cluster, address);
	}
	
	public static JGroupsDestination get(JGroupsCluster cluster, Address address) {
		
		if (address==null)
			return (JGroupsDestination)cluster.getDestination();
		
		// TODO - optimise locking here later
		JGroupsDestination destination;
		synchronized (cluster._addressToDestination) {
			destination=(JGroupsDestination)cluster._addressToDestination.get(address);
			if (destination==null) {
				destination=new JGroupsDestination(address);
				Node node=new JGroupsRemoteNode(cluster, destination, cluster.getClusterState());
				destination.init(node);
				cluster._addressToDestination.put(address, destination);
			}
		}
		return destination;
	}
	
}
