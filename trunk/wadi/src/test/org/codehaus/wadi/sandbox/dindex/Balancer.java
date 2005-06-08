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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.activecluster.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Balancer implements Runnable {

    protected final Log _log=LogFactory.getLog(getClass());

    protected final Cluster _cluster;
    protected final int _numBuckets;
    protected final Node[] _nodes;

    public Balancer(Cluster cluster, int numBuckets, Node[] nodes) {
        _cluster=cluster;
        _numBuckets=numBuckets;
        _nodes=nodes;
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

    public void run() {
        // sort Nodes into ordered sets of producers and consumers...
        List producers=new ArrayList(); // have more than they need
        List consumers=new ArrayList(); // have less than they need

        int numRemoteNodes=_nodes.length;
        int numNodes=numRemoteNodes+1;
        int numBucketsPerNode=_numBuckets/numNodes;
        // local node...
        Node localNode=_cluster.getLocalNode();
        decide(localNode, DIndexNode.getNumIndexPartitions(localNode), numBucketsPerNode, producers, consumers);
        // remote nodes...
        for (int i=0; i<numRemoteNodes; i++) {
            Node node=_nodes[i];
            int numBuckets=DIndexNode.getNumIndexPartitions(node);
            decide(node, numBuckets, numBucketsPerNode, producers, consumers);
        }
        // sort lists...
        Collections.sort(producers, new PairGreaterThanComparator());
        Collections.sort(consumers, new PairLessThanComparator());

        // account for uneven division of buckets...
        int remainingBuckets=_numBuckets%numNodes;
        ListIterator i=producers.listIterator();
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

        // now direct each producer to transfer its excess buckets to a corresponding consumer....
        Iterator p=producers.iterator();
        Iterator c=consumers.iterator();

        String correlationId=_cluster.getLocalNode().getName()+"-transfer-"+System.currentTimeMillis();

        Pair consumer=null;
        while (p.hasNext()) {
            Pair producer=(Pair)p.next();
            while (producer._deviation>0) {
                if (consumer==null)
                    consumer=c.hasNext()?(Pair)c.next():null;
                    if (producer._deviation>=consumer._deviation) {
                        transfer(producer._node, consumer._node, consumer._deviation, correlationId);
                        producer._deviation-=consumer._deviation;
                        consumer._deviation=0;
                        consumer=null;
                    } else {
                        transfer(producer._node, consumer._node, producer._deviation, correlationId);
                        consumer._deviation-=producer._deviation;
                        producer._deviation=0;
                    }
            }
        }
    }

    protected void transfer(Node src, Node tgt, int amount, String correlationId) {
        _log.info("commanding "+DIndexNode.getNodeName(src)+" to give "+amount+" to "+DIndexNode.getNodeName(tgt));
        IndexPartitionsTransferCommand command=new IndexPartitionsTransferCommand(amount, tgt.getDestination());
        try {
            ObjectMessage om=_cluster.createObjectMessage();
            om.setJMSReplyTo(_cluster.getLocalNode().getDestination());
            om.setJMSDestination(src.getDestination());
            om.setJMSCorrelationID(correlationId);
            om.setObject(command);
            _cluster.send(src.getDestination(), om);
        } catch (JMSException e) {
            _log.error("problem sending share command", e);
        }
    }

}
