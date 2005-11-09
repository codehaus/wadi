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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;

public class RemotePartition implements PartitionInterface {

	protected final Log _log=LogFactory.getLog(getClass().getName());
	protected final Destination _destination;

	public RemotePartition(Destination destination) {
		_destination=destination;
	}

	protected PartitionConfig _config;

	public void init(PartitionConfig config) {
		_config=config;
	}

	public Destination getDestination() {
		return _destination;
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

}
