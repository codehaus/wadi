/**
 * Copyright 2007 The Apache Software Foundation
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
package org.codehaus.wadi.aop.replication;

import org.codehaus.wadi.core.manager.Manager;
import org.codehaus.wadi.core.session.SessionFactory;
import org.codehaus.wadi.core.util.Streamer;
import org.codehaus.wadi.replication.manager.ReplicationManager;

/**
 * 
 * @version $Revision: 1538 $
 */
public class ClusteredStateSessionFactory implements SessionFactory {

    private final ClusteredStateAttributesFactory attributesFactory;
    private final Streamer streamer;
    private final ReplicationManager replicationManager;
    private Manager manager;
    
    public ClusteredStateSessionFactory(ClusteredStateAttributesFactory attributesFactory,
            Streamer streamer,
            ReplicationManager replicationManager) {
        if (null == attributesFactory) {
            throw new IllegalArgumentException("attributesFactory is required");
        } else if (null == streamer) {
            throw new IllegalArgumentException("streamer is required");
        } else if (null == replicationManager) {
            throw new IllegalArgumentException("replicationManager is required");
        }
        this.attributesFactory = attributesFactory;
        this.streamer = streamer;
        this.replicationManager = replicationManager;
    }

    public ClusteredStateSession create() {
        return new ClusteredStateSession(attributesFactory.create(),
            manager,
            streamer,
            replicationManager);
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }
    
}
