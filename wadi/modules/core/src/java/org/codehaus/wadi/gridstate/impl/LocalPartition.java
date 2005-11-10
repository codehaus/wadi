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

import java.util.HashMap;
import java.util.Map;

import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.gridstate.PartitionConfig;
import org.codehaus.wadi.gridstate.PartitionInterface;
import org.codehaus.wadi.impl.Utils;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;

public class LocalPartition implements PartitionInterface {

	protected static final Log _log=LogFactory.getLog(LocalPartition.class);

	protected final transient ReadWriteLock _lock;
	protected Map _map=new HashMap();

	protected PartitionConfig _config;

	public LocalPartition() {
		_lock=new ReaderPreferenceReadWriteLock();
	}

	public void init(PartitionConfig config) {
		_config=config;
	}

	public Destination getDestination() {
		return _config.getLocalDestination();
	}

	public Location getLocation(Object key) {
		try {
			Utils.safeAcquire(_lock.readLock());
			return (Location)_map.get(key);
		} finally {
			_lock.readLock().release();
		}
	}

	public ReadWriteLock getLock() {
		return _lock;
	}

	public Map getMap() {
		return _map;
	}

}
