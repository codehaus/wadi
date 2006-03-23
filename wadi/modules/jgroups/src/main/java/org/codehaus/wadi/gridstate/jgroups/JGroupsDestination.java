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
package org.codehaus.wadi.gridstate.jgroups;

import java.io.Serializable;

import javax.jms.Destination;

import org.activecluster.Node;
import org.codehaus.wadi.impl.Utils;
import org.jgroups.Address;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JGroupsDestination implements Destination, Serializable {

	protected Address _address;
  protected transient Node _node; // backptr - initialsed later...

	public JGroupsDestination(Address address) {
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

  protected static final String _prefix="<"+Utils.basename(JGroupsDestination.class)+": ";
  protected static final String _suffix=">";
  
  public String toString() {
    String name=(_node==null?"cluster":_node.getName());
    return _prefix+name+_suffix;
  }
  
}
