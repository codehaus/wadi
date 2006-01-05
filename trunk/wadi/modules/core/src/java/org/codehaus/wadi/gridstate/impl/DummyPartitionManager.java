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

import javax.jms.Destination;

import org.codehaus.wadi.gridstate.Dispatcher;
import org.codehaus.wadi.gridstate.PartitionManager;
import org.codehaus.wadi.gridstate.PartitionManagerConfig;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class DummyPartitionManager implements PartitionManager {

	protected final int _numPartitions;

	public DummyPartitionManager(int numPartitions) {
		super();
		_numPartitions=numPartitions;
	}

	public void init(PartitionManagerConfig config) {
		// empty
	}

	public void start() {
		// empty
	}

	public void stop() {
		// empty
	}

	public PartitionFacade[] getPartitions() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getNumPartitions() {
		return _numPartitions;
	}

	public PartitionFacade getPartition(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	// PartitionConfig

	public Dispatcher getDispatcher() {
		return null;
	}

	public Destination getLocalDestination() {
		return null;
	}

	public void evacuate() {
		// TODO Auto-generated method stub

	}

}