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

import org.activecluster.Cluster;
import org.activecluster.Node;

public class EvacuationBalancer extends AbstractBalancer {

    protected final Node _leaver;
    protected int _numNodes;
    protected int _numItemsPerNode;
    protected int _numRemainingItems;
    
    public EvacuationBalancer(Cluster cluster, Node leaver, Node[] nodes, int numBuckets, BalancerConfig config) {
        super(config);
        _leaver=leaver;
        }

    public void plan(Plan plan) {
        List producers=plan._producers; // have more than they need
        List consumers=plan._consumers; // have less than they need
        
        _numNodes=_remoteNodes.length;
        _numItemsPerNode=_numItems/_numNodes;
        _numRemainingItems=_numItems%_numNodes;

        // who is giving items ? - just us...
        producers.add(new Pair(_leaver, DIndexNode.getNumIndexPartitions(_leaver)));
        
        // who is receiving items ? - everyone else
        add(_cluster.getLocalNode(), consumers);
        for (int i=0; i<_numNodes; i++) {
            Node remoteNode=_remoteNodes[i];
            add(remoteNode, consumers);
        }
     }
    
    protected void add(Node node, List consumers) {
        if (node==_leaver)
            return; // ignore
        
        int deviation=_numItemsPerNode-DIndexNode.getNumIndexPartitions(node); // abstract - TODO
        if (_numRemainingItems>0) {
            _numRemainingItems--;
            deviation++;
        }
        consumers.add(new Pair(node, deviation));
    }
}
