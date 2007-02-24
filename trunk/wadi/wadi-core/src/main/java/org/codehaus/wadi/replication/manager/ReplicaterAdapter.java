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
package org.codehaus.wadi.replication.manager;

import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.core.session.Session;
import org.codehaus.wadi.replication.Replicater;
import org.codehaus.wadi.replication.ReplicaterException;

/**
 * 
 * @version $Revision$
 */
public class ReplicaterAdapter implements Replicater {
    private final ReplicationManager replicationManager;
    
    public ReplicaterAdapter(ReplicationManager replicationManager) {
        if (null == replicationManager) {
            throw new IllegalArgumentException("replicationManager is required");
        }
        this.replicationManager = replicationManager;
    }

    public void create(Object tmp) {
        Session session = castAndEnsureType(tmp);
        try {
            replicationManager.create(session.getName(), session);
        } catch (ReplicationException e) {
            throw new ReplicaterException(e);
        }
    }

    public void update(Object tmp) {
        Session session = castAndEnsureType(tmp);
        try {
            replicationManager.update(session.getName(), session);
        } catch (ReplicationException e) {
            throw new ReplicaterException(e);
        }
    }

    public void destroy(Object tmp) {
        Session session = castAndEnsureType(tmp);
        replicationManager.destroy(session.getName());
    }

    public void acquireFromOtherReplicater(Object tmp) {
        Session session = castAndEnsureType(tmp);
        Motable acquiredMotable = (Motable) replicationManager.acquirePrimary(session.getName());
        if (null == acquiredMotable) {
            return;
        }
        try {
            session.setBodyAsByteArray(acquiredMotable.getBodyAsByteArray());
        } catch (Exception e) {
            throw new ReplicaterException(e);
        }
    }
    
    public boolean getReusingStore() {
        return true;
    }
    
    private Session castAndEnsureType(Object tmp) {
        if (false == tmp instanceof Session) {
            throw new IllegalArgumentException("tmp is not a Session");
        }
        return (Session) tmp;
    }
}
