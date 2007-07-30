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
package org.codehaus.wadi.core.session;

import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.replication.manager.ReplicationManager;

/**
 * A DistributableSession enhanced with functionality associated with replication - the frequent 'backing-up' of 
 * its content to provide against catastrophic failure.
 *
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1725 $
 */
public abstract class AbstractReplicableSession extends DistributableSession {
    protected final transient ReplicationManager replicationManager;

	public AbstractReplicableSession(DistributableAttributes attributes,
            Manager manager,
            Streamer streamer,
            ReplicationManager replicationManager) {
        super(attributes, manager, streamer);
        if (null == replicationManager) {
            throw new IllegalArgumentException("replicationManager is required");
        }
        this.replicationManager = replicationManager;
    }

    public synchronized void destroy() throws Exception {
        replicationManager.destroy(getName());
        super.destroy();
    }

}
