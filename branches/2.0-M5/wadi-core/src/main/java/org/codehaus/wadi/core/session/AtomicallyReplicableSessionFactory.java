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

import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.replication.manager.ReplicationManager;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision: 1885 $
 */
public class AtomicallyReplicableSessionFactory extends DistributableSessionFactory {

    protected final ReplicationManager replicationManager;
    
    public AtomicallyReplicableSessionFactory(AttributesFactory attributesFactory,
            Streamer streamer,
            ReplicationManager replicationManager) {
        super(attributesFactory, streamer);
        if (null == replicationManager) {
            throw new IllegalArgumentException("replicationManager is required");
        }
        this.replicationManager = replicationManager;
    }

    public Session create() {
        return new AtomicallyReplicableSession(newAttributes(), getManager(), streamer, replicationManager);
    }
    
}

