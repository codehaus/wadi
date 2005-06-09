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
package org.codehaus.wadi.sandbox.dindex;

import java.util.List;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.activecluster.Node;

public class EvacuationBalancer extends AbstractBalancer {

    public EvacuationBalancer(Cluster cluster, Node[] nodes, int numBuckets) {
        super(cluster, nodes, numBuckets);
    }

    public void plan(Plan plan) {
        List producers=plan._producers; // have more than they need
        List consumers=plan._consumers; // have less than they need
        
        // who is giving items ? - just us...
        Node localNode=_cluster.getLocalNode();
        producers.add(new Pair(localNode, DIndexNode.getNumIndexPartitions(localNode)));
        
        // who is receiving items ? - everyone else
        int numRemoteNodes=_nodes.length;
        int numItemsPerNode=_numItems/numRemoteNodes;
        int numRemainingItems=_numItems%numRemoteNodes;
        
        for (int i=0; i<numRemoteNodes; i++) {
            Node remoteNode=_nodes[i];
            int deviation=numItemsPerNode-DIndexNode.getNumIndexPartitions(remoteNode); // abstract - TODO
            if (numRemainingItems>0) {
                numRemainingItems--;
                deviation++;
            }
            consumers.add(new Pair(remoteNode, deviation));
        }
     }
    
    protected void transfer(Node src, Node tgt, int amount, String correlationId) {
        _log.info(DIndexNode.getNodeName(src)+" evacuating "+amount+" items to "+DIndexNode.getNodeName(tgt));
    }
}
