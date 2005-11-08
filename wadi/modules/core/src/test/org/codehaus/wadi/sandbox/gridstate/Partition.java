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
package org.codehaus.wadi.sandbox.gridstate;

import java.util.Map;

import javax.jms.Destination;

import org.jgroups.Address;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;

public class Partition implements PartitionInterface {

	protected final PartitionInterface _partition;

	Partition(PartitionInterface partition) {
		_partition=partition;
	}

	public void init(PartitionConfig config) {
		_partition.init(config);
	}

	public Destination getDestination() {
		return _partition.getDestination();
	}

	public Address getAddress() {
		return _partition.getAddress();
	}

	public Location getLocation(Object key) {
		return _partition.getLocation(key);
	}

	public ReadWriteLock getLock() {
		return _partition.getLock();
	}

	public Map getMap() {
		return _partition.getMap();
	}

}
