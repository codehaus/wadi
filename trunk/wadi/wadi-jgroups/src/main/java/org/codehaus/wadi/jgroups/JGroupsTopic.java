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
import org.codehaus.wadi.group.Peer;
import org.jgroups.Address;

public class JGroupsTopic extends JGroupsAddress {

  protected final String _name;

  public JGroupsTopic(String name, Address address) {
    super(address); // null Node
    _name=name;
  }

  public void init(Peer node) {
  }

	protected Object readResolve() throws ObjectStreamException {
		JGroupsCluster cluster=(JGroupsCluster)JGroupsCluster._cluster.get();
		return cluster.getAddress();
	}
}
