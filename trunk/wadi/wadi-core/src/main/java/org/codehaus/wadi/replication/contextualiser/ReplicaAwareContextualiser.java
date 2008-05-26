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
package org.codehaus.wadi.replication.contextualiser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.core.contextualiser.AbstractSharedContextualiser;
import org.codehaus.wadi.core.contextualiser.Contextualiser;
import org.codehaus.wadi.core.motable.Emoter;
import org.codehaus.wadi.core.motable.Immoter;
import org.codehaus.wadi.core.motable.Motable;
import org.codehaus.wadi.location.statemanager.StateManager;
import org.codehaus.wadi.replication.manager.ReplicationManager;

/**
 * 
 * @version $Revision: 1603 $
 */
public class ReplicaAwareContextualiser extends AbstractSharedContextualiser {
    private static final Log log = LogFactory.getLog(ReplicaAwareContextualiser.class);
    
    private final ReplicationManager replicationManager;
    private final StateManager stateManager;
    
    public ReplicaAwareContextualiser(Contextualiser next,
            ReplicationManager replicationManager,
            StateManager stateManager) {
        super(next);
        if (null == replicationManager) {
            throw new IllegalArgumentException("replicationManager is required");
        } else if (null == stateManager) {
            throw new IllegalArgumentException("stateManager is required");
        }
        this.replicationManager = replicationManager;
        this.stateManager = stateManager;
    }

    public Emoter getEmoter() {
        return new PromotionEmoter();
    }

    public Immoter getImmoter() {
        return next.getSharedDemoter();
    }
    
    protected Motable get(String id, boolean exclusiveOnly) {
        Motable motable;
        try {
            motable = replicationManager.retrieveReplica(id);
        } catch (Exception e) {
            return null;
        }
        if (null != motable) {
            stateManager.insert(motable.getName());
        }
        return motable;
    }

    private final class PromotionEmoter implements Emoter {
        public boolean emote(Motable emotable, Motable immotable) {
            try {
                immotable.restore(emotable.getCreationTime(), 
                        emotable.getLastAccessedTime(),
                        emotable.getMaxInactiveInterval(),
                        emotable.getName(),
                        emotable.getBodyAsByteArray());
            } catch (Exception e) {
                log.warn("Problem emoting [" + emotable + "]", e);
                return false;
            }

            return true;
        }
    }
    
}
