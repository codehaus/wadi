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
import java.util.Iterator;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.activecluster.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// it's important that the Plan is constructed from snapshotted resources (i.e. the ground doesn't
// shift under its feet), and that it is made and executed as quickly as possible - as a node could 
// leave the Cluster in the meantime...

public abstract class AbstractBalancer implements Runnable {

    protected final Log _log=LogFactory.getLog(getClass());
    protected final Cluster _cluster;
    protected final Node[] _nodes;
    protected final int _numItems;
    
    class Plan {
        public List _producers=new ArrayList();
        public List _consumers=new ArrayList();
    }
    
    public AbstractBalancer(Cluster cluster, Node[] nodes, int numItems) {
        _cluster=cluster;
        _numItems=numItems;
        _nodes=nodes;
    }

    public void run() {
        Plan plan=new Plan();
        // plan what is going where...
        plan(plan);
        // execute the plan
        execute(plan);
    }

    protected abstract void plan(Plan plan);

    protected void execute(Plan plan)
     {
        List producers=plan._producers;
        List consumers=plan._consumers;
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
