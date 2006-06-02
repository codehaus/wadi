/**
 * Copyright 2006 The Apache Software Foundation
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
package org.codehaus.wadi.replication.manager.basic;

import org.codehaus.wadi.RehydrationException;
import org.codehaus.wadi.replication.ReplicationException;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.ReplicationManager;
import org.codehaus.wadi.web.WebSession;

/**
 * 
 * @version $Revision: 1603 $
 */
public class SessionReplicationManager implements ReplicationManager {
    private final ReplicationManager replicationManager;
    private final SessionRehydrater rehydrater;
    
    public SessionReplicationManager(ReplicationManager replicationManager, SessionRehydrater rehydrater) {
        this.replicationManager = replicationManager;
        this.rehydrater = rehydrater;
    }

    public void create(Object key, Object tmp) {
        WebSession session = castAndEnsureType(tmp);
        try {
            replicationManager.create(key, session.getBodyAsByteArray());
        } catch (Exception e) {
            throw new ReplicationException(e);
        }
    }

    public void update(Object key, Object tmp) {
        WebSession session = castAndEnsureType(tmp);
        try {
            replicationManager.update(key, session.getBodyAsByteArray());
        } catch (Exception e) {
            throw new ReplicationException(e);
        }
    }

    public void destroy(Object key) {
        replicationManager.destroy(key);
    }

    public Object acquirePrimary(Object key) {
        if (false == key instanceof String) {
            throw new IllegalArgumentException("key must be a string. Was :" +
                    key.getClass().getName());
        }
        
        byte[] body = (byte[]) replicationManager.acquirePrimary(key);
        if (null == body) {
            return null;
        }
        
        try {
            return rehydrater.rehydrate((String) key, body);
        } catch (RehydrationException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        }
    }

    public ReplicaInfo releasePrimary(Object key) {
        return replicationManager.releasePrimary(key);
    }

    public ReplicaInfo retrieveReplicaInfo(Object key) {
        return replicationManager.retrieveReplicaInfo(key);
    }

    public boolean managePrimary(Object key) {
        return replicationManager.managePrimary(key);
    }

    public void start() throws Exception {
        replicationManager.start();
    }

    public void stop() throws Exception {
        replicationManager.stop();
    }

    private WebSession castAndEnsureType(Object tmp) {
        if (false == tmp instanceof WebSession) {
            throw new IllegalArgumentException("tmp is not a Session");
        }
        return (WebSession) tmp;
    }
}
