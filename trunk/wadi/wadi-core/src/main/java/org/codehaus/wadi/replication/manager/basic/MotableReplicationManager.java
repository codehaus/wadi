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

import org.codehaus.wadi.Motable;
import org.codehaus.wadi.core.motable.SimpleMotable;
import org.codehaus.wadi.replication.common.ReplicaInfo;
import org.codehaus.wadi.replication.manager.InternalReplicationManagerException;
import org.codehaus.wadi.replication.manager.ReplicationKeyNotFoundException;
import org.codehaus.wadi.replication.manager.ReplicationManager;

/**
 * 
 * @version $Revision: 1603 $
 */
public class MotableReplicationManager implements ReplicationManager {
    private static final int maxInactiveInterval =  30 * 60;
    
    private final ReplicationManager replicationManager;
    
    public MotableReplicationManager(ReplicationManager replicationManager) {
        if (null == replicationManager) {
            throw new IllegalArgumentException("replicationManager is required");
        }
        this.replicationManager = replicationManager;
    }

    public void create(Object key, Object tmp) {
        Motable motable = castAndEnsureType(tmp);
        byte[] body;
        try {
            body = motable.getBodyAsByteArray();
        } catch (Exception e) {
            throw new InternalReplicationManagerException(e);
        }
        replicationManager.create(key, body);
    }

    public void update(Object key, Object tmp) {
        Motable motable = castAndEnsureType(tmp);
        byte[] body;
        try {
            body = motable.getBodyAsByteArray();
        } catch (Exception e) {
            throw new InternalReplicationManagerException(e);
        }
        replicationManager.update(key, body);
    }

    public void destroy(Object key) {
        replicationManager.destroy(key);
    }

    public Object acquirePrimary(Object key) throws ReplicationKeyNotFoundException, InternalReplicationManagerException {
        if (false == key instanceof String) {
            throw new IllegalArgumentException("key must be a string. Was [" + key.getClass().getName() + "]");
        }
        
        byte[] body = (byte[]) replicationManager.acquirePrimary(key);
        if (null == body) {
            return null;
        }
        
        SimpleMotable motable = new SimpleMotable();
        long time = System.currentTimeMillis();
        try {
            motable.restore(time, time, maxInactiveInterval, (String) key, body);
        } catch (Exception e) {
            throw new InternalReplicationManagerException(e);
        }
        return motable;
    }

    public boolean releasePrimary(Object key) {
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

    private Motable castAndEnsureType(Object tmp) {
        if (false == tmp instanceof Motable) {
            throw new IllegalArgumentException("tmp is not a Motable");
        }
        return (Motable) tmp;
    }
}
