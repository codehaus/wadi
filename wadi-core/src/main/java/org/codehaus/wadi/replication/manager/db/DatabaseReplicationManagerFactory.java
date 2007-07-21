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
package org.codehaus.wadi.replication.manager.db;

import org.codehaus.wadi.core.store.DatabaseStore;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.replication.manager.ReplicationManagerFactory;
import org.codehaus.wadi.replication.strategy.BackingStrategyFactory;
import org.codehaus.wadi.servicespace.ServiceSpace;

/**
 * A DatabaseReplicater holds no per Session state, so all Sessions may use the same Replicater
 *
 */
/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 2293 $
 */
public class DatabaseReplicationManagerFactory implements ReplicationManagerFactory {

	private final DatabaseStore store;

    public DatabaseReplicationManagerFactory(DatabaseStore store) {
        this.store = store;
	}

    public ReplicationManager factory(ServiceSpace serviceSpace, BackingStrategyFactory backingStrategyFactory) {
        return new DatabaseReplicationManager(store);
    }
}
