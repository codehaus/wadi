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
package org.codehaus.wadi.gridstate.impl;

import java.util.Map;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.gridstate.PartitionConfig;
import org.codehaus.wadi.gridstate.Partition;
import org.codehaus.wadi.group.Address;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class RemotePartition implements Partition {

	protected final Log _log=LogFactory.getLog(getClass().getName());
	protected final Address _address;

	public RemotePartition(Address address) {
		_address=address;
	}

	protected PartitionConfig _config;

	public void init(PartitionConfig config) {
		_config=config;
	}

	public Address getAddress() {
		return _address;
	}

	public Location getLocation(Object key) {
		throw new UnsupportedOperationException("What should we do here?");
	}

	public ReadWriteLock getLock() {
		throw new UnsupportedOperationException("What should we do here?");
	}

	public Map getMap() {
		throw new UnsupportedOperationException("What should we do here?");
	}


	// PMPartition API

	public boolean isLocal() {
		return false;
	}

	public int getKey() {
		throw new UnsupportedOperationException("NYI");
	}

}
