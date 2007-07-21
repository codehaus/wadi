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
package org.codehaus.wadi.replication.strategy;

import java.util.LinkedList;
import java.util.List;

import org.codehaus.wadi.group.Peer;


/**
 * 
 * @version $Revision$
 */
public class RoundRobinBackingStrategy implements BackingStrategy {
    private static final Peer[] EMPTY_NODES = new Peer[0];
    
    private final int nbReplica;
    private final List secondaries;
    private int lastReplicaIndex;
    
    public RoundRobinBackingStrategy(int nbReplica) {
        if (nbReplica < 1) {
            throw new IllegalArgumentException("nbReplica must be greater than 0");
        }
        this.nbReplica = nbReplica;
        
        secondaries = new LinkedList();
    }

    public Peer[] electSecondaries(Object key) {
        Peer[] result = new Peer[nbReplica];
        int resultIndex = 0;
        int initialReplicaIndex =  lastReplicaIndex;
        boolean looped = false;
        synchronized (secondaries) {
            if (0 == secondaries.size()) {
                return EMPTY_NODES;
            }
            while (resultIndex < nbReplica) {
                if (lastReplicaIndex >= secondaries.size()) {
                    lastReplicaIndex = 0;
                    if (looped) {
                        break;
                    }
                    looped = true;
                }
                if (lastReplicaIndex == initialReplicaIndex && looped) {
                    break;
                }
                result[resultIndex++] = (Peer) secondaries.get(lastReplicaIndex++);
            }
        }
        
        if (resultIndex < nbReplica) {
            Peer[] resizedResult = new Peer[resultIndex];
            System.arraycopy(result, 0, resizedResult, 0, resultIndex);
            result = resizedResult;
        }
        
        return result;
    }

    public Peer[] reElectSecondaries(Object key, Peer primary, Peer[] secondaries) {
        return electSecondaries(key);
    }

    public void addSecondaries(Peer[] newSecondaries) {
        synchronized (secondaries) {
            for (int i = 0; i < newSecondaries.length; i++) {
                secondaries.add(newSecondaries[i]);
            }
        }
    }
    
    public void addSecondary(Peer secondary) {
        synchronized (secondaries) {
            secondaries.add(secondary);
        }
    }

    public void removeSecondary(Peer secondary) {
        synchronized (secondaries) {
            secondaries.remove(secondary);
        }
    }
    
    public void reset() {
        synchronized (secondaries) {
            secondaries.clear();
        }
    }
    
}
