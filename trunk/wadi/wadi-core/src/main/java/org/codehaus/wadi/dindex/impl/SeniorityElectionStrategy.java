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

import org.apache.activecluster.Cluster;
import org.apache.activecluster.Node;
import org.apache.activecluster.election.ElectionStrategy;

/**
 * @author <a href="mailto:jules@coredevelopers.net">Jules Gosnell</a>
 * @version $Revision$
 */
public class SeniorityElectionStrategy implements ElectionStrategy {

    public Node doElection(Cluster cluster) {
        Node oldest=cluster.getLocalNode();
        long earliest=getBirthTime(oldest);
        for (Iterator i=cluster.getNodes().values().iterator(); i.hasNext();) {
            Node candidate=(Node)i.next();
            long birthTime=getBirthTime(candidate);
            if (birthTime<earliest) {
                earliest=birthTime;
                oldest=candidate;
            }
        }

        return oldest;
    }

    protected long getBirthTime(Node node) {
        return ((Long)node.getState().get("birthTime")).longValue(); // TODO - unify state keys somewhere
    }

}
