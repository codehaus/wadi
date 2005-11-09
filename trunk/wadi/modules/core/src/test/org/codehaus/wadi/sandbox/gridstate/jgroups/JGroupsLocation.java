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
package org.codehaus.wadi.sandbox.gridstate.jgroups;

import org.codehaus.wadi.sandbox.gridstate.AbstractLocation;
import org.jgroups.Address;

public class JGroupsLocation extends AbstractLocation {

	protected Address _address;

	public JGroupsLocation(Address address) {
		super();
		_address=address;
	}

	public Object getValue() {
		return _address;
	}

	public void setValue(Object address) {
		_address=(Address)address;
	}
}
