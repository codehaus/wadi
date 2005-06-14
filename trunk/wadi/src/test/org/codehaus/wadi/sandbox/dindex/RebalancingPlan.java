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
    
    public RebalancingPlan(Node[] living, Node[] leaving, int numItems) {
        int numBucketsPerNode=numItems/living.length;

        for (int i=0; i<leaving.length; i++) {
            Node node=leaving[i];
            int numBuckets=DIndexNode.getNumIndexPartitions(node);
            System.out.println("leaving: "+DIndexNode.getNodeName(node)+" has "+numBuckets);
            if (numBuckets>0)
                _producers.add(new Pair(node, numBuckets));
        }

        for (int i=0; i<living.length; i++) {
            Node node=living[i];
            int numBuckets=DIndexNode.getNumIndexPartitions(node);
            System.out.println("living : "+DIndexNode.getNodeName(node)+" has "+numBuckets);
            decide(node, numBuckets, numBucketsPerNode, _producers, _consumers);
        }
        
        
        // sort lists...
        Collections.sort(_producers, new PairGreaterThanComparator());
        Collections.sort(_consumers, new PairLessThanComparator());
        
        // account for uneven division of buckets...
        int remainingBuckets=numItems%living.length;
        ListIterator i=_producers.listIterator();
        while(remainingBuckets>0 && i.hasNext()) {
            Pair p=(Pair)i.next();
            remainingBuckets--;
            if ((--p._deviation)==0)
                i.remove();
        }

        assert remainingBuckets==0;
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
