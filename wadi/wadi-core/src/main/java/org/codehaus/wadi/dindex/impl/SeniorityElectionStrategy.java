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
package org.codehaus.wadi.dindex.impl;

import java.util.Iterator;
import org.codehaus.wadi.group.Cluster;
import org.codehaus.wadi.group.ElectionStrategy;
import org.codehaus.wadi.group.Peer;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SeniorityElectionStrategy implements ElectionStrategy {

    public Peer doElection(Cluster cluster) {
        Peer oldest = cluster.getLocalPeer();
        long earliest = getBirthTime(oldest);
        for (Iterator i = cluster.getRemotePeers().values().iterator(); i.hasNext();) {
            Peer candidate = (Peer) i.next();
            long birthTime = getBirthTime(candidate);
            if (birthTime < earliest) {
                earliest = birthTime;
                oldest = candidate;
            }
        }

        return oldest;
    }

    protected long getBirthTime(Peer node) {
        Long birthTimeAsLong = (Long) node.getState().get("birthTime");
        if (null == birthTimeAsLong) {
            return Long.MAX_VALUE;
        }
        
        return ((Long)node.getState().get("birthTime")).longValue(); // TODO - unify state keys somewhere
    }
}