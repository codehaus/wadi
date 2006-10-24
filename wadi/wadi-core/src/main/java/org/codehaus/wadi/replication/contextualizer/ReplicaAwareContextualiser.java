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
package org.codehaus.wadi.replication.contextualizer;

import org.codehaus.wadi.Contextualiser;
import org.codehaus.wadi.Emoter;
import org.codehaus.wadi.Immoter;
import org.codehaus.wadi.Motable;
import org.codehaus.wadi.impl.AbstractSharedContextualiser;
import org.codehaus.wadi.impl.RWLocker;
import org.codehaus.wadi.replication.manager.ReplicationException;
import org.codehaus.wadi.replication.manager.ReplicationManager;

/**
 * 
 * @version $Revision: 1603 $
 */
public class ReplicaAwareContextualiser extends AbstractSharedContextualiser {
    private final ReplicationManager replicationManager;
    
    public ReplicaAwareContextualiser(Contextualiser next, ReplicationManager replicationManager) {
        super(next, new RWLocker(), false);
        if (null == replicationManager) {
            throw new IllegalArgumentException("replicationManager is required");
        }
        this.replicationManager = replicationManager;
    }

    public Emoter getEmoter() {
        return new PromotionEmoter();
    }

    public Immoter getImmoter() {
        return _next.getSharedDemoter();
    }

    public Motable get(String id) {
        Object object;
        try {
            object = replicationManager.acquirePrimary(id);
        } catch (ReplicationException e) {
            throw (IllegalStateException) new IllegalStateException().initCause(e);
        }
        return (Motable) object;
    }

    public void load(Emoter emoter, Immoter immoter) {
    }
    
    private final class PromotionEmoter implements Emoter {
        public boolean prepare(String name, Motable emotable, Motable immotable) {
            immotable.init(emotable.getCreationTime(), 
                    emotable.getLastAccessedTime(),
                    emotable.getMaxInactiveInterval(),
                    name);
            try {
                immotable.setBodyAsByteArray(emotable.getBodyAsByteArray());
            } catch (Exception e) {
                _log.warn("Problem emoting " + name, e);
                return false;
            }

            return true;
        }

        public void commit(String name, Motable motable) {
        }

        public void rollback(String name, Motable motable) {
        }

        public String getInfo() {
            return null;
        }
    }
    
}
