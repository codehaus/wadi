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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.codehaus.wadi.replication.common.NodeInfo;


/**
 * 
 * @version $Revision$
 */
public class RoundRobinBackingStrategy implements BackingStrategy {
    private static final NodeInfo[] EMPTY_NODES = new NodeInfo[0];
    
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

    public NodeInfo[] electSecondaries(Object key) {
        NodeInfo[] result = new NodeInfo[nbReplica];
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
                result[resultIndex++] = (NodeInfo) secondaries.get(lastReplicaIndex++);
            }
        }
        
        if (resultIndex < nbReplica) {
            NodeInfo[] resizedResult = new NodeInfo[resultIndex];
            System.arraycopy(result, 0, resizedResult, 0, resultIndex);
            result = resizedResult;
        }
        
        return result;
    }

    public NodeInfo[] reElectSecondaries(Object key, NodeInfo primary, NodeInfo[] secondaries) {
        return electSecondaries(key);
    }

    public void addSecondaries(NodeInfo[] newSecondaries) {
        synchronized (secondaries) {
            secondaries.addAll(Arrays.asList(newSecondaries));
        }
    }
    
    public void addSecondary(NodeInfo secondary) {
        synchronized (secondaries) {
            secondaries.add(secondary);
        }
    }

    public void removeSecondary(NodeInfo secondary) {
        synchronized (secondaries) {
            secondaries.remove(secondary);
        }
    }
}
