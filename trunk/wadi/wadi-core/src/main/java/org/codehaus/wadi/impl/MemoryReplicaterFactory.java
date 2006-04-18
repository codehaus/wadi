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
package org.codehaus.wadi.impl;

import org.codehaus.wadi.Replicater;
import org.codehaus.wadi.ReplicaterFactory;

/**
 * MemoryReplicaters hold per Session state (the location of their replication partners), so we need to create a new
 * MemoryReplicater for each session.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 *
 */
public class MemoryReplicaterFactory implements ReplicaterFactory {

	protected int _numReplicants;

	public MemoryReplicaterFactory(int numReplicants) {
		_numReplicants=numReplicants;
	}

	public Replicater create() {
		return new MemoryReplicater(_numReplicants);
	}

}
