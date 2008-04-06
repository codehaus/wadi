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

import org.codehaus.wadi.group.LocalPeer;
import org.codehaus.wadi.group.Peer;
import org.codehaus.wadi.servicespace.ServiceSpace;


/**
 * 
 * @version $Revision$
 */
public class RoundRobinBackingStrategy implements BackingStrategy {
    private static final Peer[] EMPTY_NODES = new Peer[0];
    
    private final LocalPeer localPeer;
    private final int nbReplica;
    private final List<Peer> secondaries;
    private int lastReplicaIndex;

    
    public RoundRobinBackingStrategy(ServiceSpace serviceSpace, int nbReplica) {
        if (null == serviceSpace) {
            throw new IllegalArgumentException("serviceSpace is required");
        } else if (nbReplica < 1) {
            throw new IllegalArgumentException("nbReplica must be greater than 0");
        }
        this.nbReplica = nbReplica;
        
        localPeer = serviceSpace.getLocalPeer();
        secondaries = new LinkedList<Peer>();
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
                result[resultIndex++] = secondaries.get(lastReplicaIndex++);
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

    public Peer[] reElectSecondariesForSwap(Object key, Peer newPrimary, Peer[] secondaries) {
        Peer[] newSecondaries = secondaries;
        
        boolean newPrimaryWasSecondary = false;
        for (int i = 0; i < newSecondaries.length; i++) {
            if (newSecondaries[i].equals(newPrimary)) {
                newSecondaries[i] = localPeer;
                newPrimaryWasSecondary = true;
                break;
            }
        }
        
        if (!newPrimaryWasSecondary && newSecondaries.length < nbReplica) {
            Peer[] tmpNewSecondaries = new Peer[newSecondaries.length + 1];
            System.arraycopy(newSecondaries, 0, tmpNewSecondaries, 0, newSecondaries.length);
            tmpNewSecondaries[newSecondaries.length] = localPeer;
            newSecondaries = tmpNewSecondaries;
        }
        
        return newSecondaries;
    }
    
    public void addSecondaries(Peer[] newSecondaries) {
        synchronized (secondaries) {
            for (int i = 0; i < newSecondaries.length; i++) {
                addSecondary(newSecondaries[i]);
            }
        }
    }
    
    public void addSecondary(Peer secondary) {
        synchronized (secondaries) {
            if (!secondaries.contains(secondary) && !localPeer.equals(secondary)) {
                secondaries.add(secondary);
            }
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
