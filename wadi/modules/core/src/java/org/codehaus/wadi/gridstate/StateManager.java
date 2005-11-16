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
package org.codehaus.wadi.gridstate;

import javax.jms.Destination;

public interface StateManager {

	void init(StateManagerConfig config);

	// called on IM...
	Object get(Object key);

	// called on IM...
	Object put(Object key, Object value, boolean overwrite, boolean returnOldValue);

	Object remove(Object key, boolean returnOldValue);

	PartitionInterface createRemotePartition();

    void start() throws Exception;

    void stop() throws Exception;

	Object syncRpc(Destination destination, Object message) throws Exception;

	//Object getLocalLocation();
}
