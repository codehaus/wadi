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

import org.jgroups.Address;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class JGroupsDestination implements Destination, Serializable {

	protected final Address _address;

	public JGroupsDestination(Address address) {
		_address=address;
	}

	public Address getAddress() {
		return _address;
	}

	// TODO - custom Serialisation - consider ensuring Object Identity between different incoming instances of this class
	// it would save [de]allocation but impact concurrency....

}
