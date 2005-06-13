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

import java.util.Collection;
import java.util.Collections;
import java.util.ListIterator;

import org.activecluster.Node;

public class RebalancingPlan extends Plan {
    
    protected boolean contains(Node[] nodes, Node node) {
        for (int i=0; i<nodes.length; i++) {
            if (nodes[i].getDestination().equals(node.getDestination()))
                System.out.println("MATCH!");
                return true;
        }
        return false;
    }

    protected int exclude(Node[] remoteNodes, Node[] leavers) {
        int count=0;
        for (int i=0; i<remoteNodes.length; i++)
            if (contains(leavers, remoteNodes[i]))
                count++;
        return count;
    }
    
    public RebalancingPlan(Node localNode, Node[] remoteNodes, Node[] leavers, int numItems) {
        int numRemoteNodes=remoteNodes.length-exclude(remoteNodes, leavers);
        int numNodes=numRemoteNodes+1;
        int numBucketsPerNode=numItems/numNodes;
        // local node...
        decide(localNode, DIndexNode.getNumIndexPartitions(localNode), numBucketsPerNode, _producers, _consumers);
        // remote nodes...
        for (int i=0; i<numRemoteNodes; i++) {
            Node node=remoteNodes[i];
            int numBuckets=DIndexNode.getNumIndexPartitions(node);
            System.out.println(DIndexNode.getNodeName(node)+" has "+numBuckets);
            if (!contains(leavers, node))
                decide(node, numBuckets, numBucketsPerNode, _producers, _consumers);
            else
                if (numBuckets>0)
                    _producers.add(new Pair(node, numBuckets));
        }
        // sort lists...
        Collections.sort(_producers, new PairGreaterThanComparator());
        Collections.sort(_consumers, new PairLessThanComparator());
        
        // account for uneven division of buckets...
        int remainingBuckets=numItems%numNodes;
        ListIterator i=_producers.listIterator();
        while(remainingBuckets>0 && i.hasNext()) {
            Pair p=(Pair)i.next();
            remainingBuckets--;
            if ((--p._deviation)==0)
                i.remove();
        }
        assert remainingBuckets==0;
        
        // above is good for addNode
        // when a node leaves cleanly, we need to run this the other way around
        // so that the remainder is added to the smallest consumers, rather than taken from the largest producers...
    }

    protected void decide(Node node, int numBuckets, int numBucketsPerNode, Collection producers, Collection consumers) {
        int deviation=numBuckets-numBucketsPerNode;
        if (deviation>0) {
            producers.add(new Pair(node, deviation));
            return;
        }
        if (deviation<0) {
            consumers.add(new Pair(node, -deviation));
            return;
        }
    }


}
