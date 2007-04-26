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
package org.codehaus.wadi.location.partitionmanager;

import org.codehaus.wadi.core.Lifecycle;
import org.codehaus.wadi.location.balancing.PartitionBalancingInfo;

/**
 * A PartitionManager is responsible for unambiguously renegotiating Partition ownership every time
 * that there is a change in Cluster membership and exposing these Partitions to the rest of the program,
 * whether local or Remote.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 *
 */
public interface PartitionManager extends Lifecycle {
	Partition getPartition(Object key);
	
    PartitionBalancingInfo getBalancingInfo();
	
	void evacuate() throws Exception;
}
