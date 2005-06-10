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


import org.activecluster.Node;


public class EvacuationPlan extends Plan {

    public EvacuationPlan(Node leaver, Node localNode, Node[] remoteNodes, int numItems) {
        int numNodes=remoteNodes.length;
        int numItemsPerNode=numItems/numNodes;
        int numRemainingItems=numItems%numNodes;

        // who is giving items ? - just us...
        _producers.add(new Pair(leaver, DIndexNode.getNumIndexPartitions(leaver)));
        
        // who is receiving items ? - everyone else
        add(leaver, numItemsPerNode, numRemainingItems, localNode);
        for (int i=0; i<numNodes; i++) {
            Node remoteNode=remoteNodes[i];
            add(leaver, numItemsPerNode, numRemainingItems, remoteNode);
        }
     }
    
    protected void add(Node leaver, int numItemsPerNode, int numRemainingItems, Node node) {
        if (node==leaver)
            return; // ignore
        
        int deviation=numItemsPerNode-DIndexNode.getNumIndexPartitions(node); // abstract - TODO
        if (numRemainingItems>0) {
            numRemainingItems--;
            deviation++;
        }
        _consumers.add(new Pair(node, deviation));
    }
}
