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
import java.util.Iterator;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.activecluster.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.Quipu;

import EDU.oswego.cs.dl.util.concurrent.Rendezvous;
import EDU.oswego.cs.dl.util.concurrent.Slot;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;
import EDU.oswego.cs.dl.util.concurrent.WaitableBoolean;
import EDU.oswego.cs.dl.util.concurrent.WaitableInt;

//it's important that the Plan is constructed from snapshotted resources (i.e. the ground doesn't
//shift under its feet), and that it is made and executed as quickly as possible - as a node could
//leave the Cluster in the meantime...

public class Coordinator implements Runnable {

    protected final Log _log=LogFactory.getLog(getClass());

    protected final Slot _flag=new Slot();

    protected final CoordinatorConfig _config;
    protected final Cluster _cluster;
    protected final Node _localNode;
    protected final int _numItems;
    protected final long _heartbeat=5000L; // TODO - unify with _cluster...

    public Coordinator(CoordinatorConfig config) {
        _config=config;
        _cluster=_config.getCluster();
        _localNode=_cluster.getLocalNode();
        _numItems=_config.getNumItems();
    }

    protected Thread _thread;
    protected Node[] _remoteNodes;


    public synchronized void start() throws Exception {
        _log.info("starting...");
        _thread=new Thread(this, "WADI Coordinator");
        _thread.start();
        _log.info("...started");
    }

    public synchronized void stop() throws Exception {
        // somehow wake up thread
        _log.info("stopping...");
        _flag.put(Boolean.FALSE);
        _thread.join();
        _thread=null;
        _log.info("...stopped");
    }

    public synchronized void queueRebalancing() {
        _log.info("queueing rebalancing...");
        try {
            _flag.offer(Boolean.TRUE, 0);
        } catch (InterruptedException e) {
            _log.warn("unexpected interruption");
        }
        _log.info("...rebalancing queued");
    }

    public void run() {
        try {
            while (_flag.take()==Boolean.TRUE) {
                rebalanceClusterState();
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
            _log.info("interrupted"); // hmmm.... - TODO
        }
    }

    // should loop until it gets a successful outcome - emptying _flag each time...
    public void rebalanceClusterState() {

        Map nodeMap=_cluster.getNodes();

        Collection livingNodes=nodeMap.values();
        synchronized (livingNodes) {livingNodes=new ArrayList(livingNodes);} // snapshot
        livingNodes.add(_cluster.getLocalNode());
        
 
        Collection l=_config.getLeavers();
        synchronized (l) {l=new ArrayList(l);} // snapshot
        
        Collection leavingNodes=new ArrayList();
        for (Iterator i=l.iterator(); i.hasNext(); ) {
            Node node=(Node)i.next();
            Node leaver=(Node)nodeMap.get(node.getDestination()); // convert into same instance as liveNodes
            if (leaver!=null) {
                leavingNodes.add(leaver);
                livingNodes.remove(leaver);
            }
        }
        
        Node [] living=(Node[])livingNodes.toArray(new Node[livingNodes.size()]);
        Node [] leaving=(Node[])leavingNodes.toArray(new Node[leavingNodes.size()]);

        String correlationId=_localNode.getName()+"-balancing-"+System.currentTimeMillis();
        Plan plan=new RebalancingPlan(living, leaving, _numItems);

        Map rvMap=_config.getRendezVousMap();
        Quipu rv=new Quipu(0);
        rvMap.put(correlationId, rv);
        execute(plan, correlationId, rv); // quipu will be incremented as participants are invited

        boolean success=false;
        try {
            _log.info("WAITING ON RENDEZVOUS");
            if (rv.waitFor(_heartbeat)) {
                _log.info("RENDEZVOUS SUCCESSFUL");
                Collection results=rv.getResults();
                success=true;
            } else {
                _log.info("RENDEZVOUS FAILED");
            }
        } catch (TimeoutException e) {
            _log.warn("timed out waiting for response", e);
        } catch (InterruptedException e) {
            _log.warn("unexpected interruption", e);
        } finally {
            rvMap.remove(correlationId);
            // somehow check all returned success..
            
            // send EvacuationResponses to each leaving node... - hmmm....
            for (int i=0; i<leaving.length; i++) {
                Node node=leaving[i];
                _log.info("acknowledging evacuation of "+DIndexNode.getNodeName(node));
                EvacuationResponse response=new EvacuationResponse();
                try {
                    ObjectMessage om=_cluster.createObjectMessage();
                    om.setJMSReplyTo(_cluster.getLocalNode().getDestination());
                    om.setJMSDestination(node.getDestination());
                    om.setJMSCorrelationID(node.getName());
                    om.setObject(response);
                    _cluster.send(node.getDestination(), om);
                } catch (JMSException e) {
                    _log.error("problem sending EvacuationResponse to "+DIndexNode.getNodeName(node), e);
                }
            }
        }

        // EvacuationRequest places a node on excludedNodes...
        // nodeFailed removes it....
        // not just on Coordinator...

        printNodes(_localNode, _cluster.getNodes());

    }

    protected void printNodes(Node localNode, Node[] remoteNodes) {
        _log.info(DIndexNode.getNodeName(localNode)+" : "+DIndexNode.getNumIndexPartitions(localNode));

        int n=remoteNodes.length;
        for (int i=0; i<n; i++) {
            Node remoteNode=remoteNodes[i];
            _log.info(DIndexNode.getNodeName(remoteNode)+" : "+DIndexNode.getNumIndexPartitions(remoteNode)+" - "+remoteNode);
        }
    }

    protected void printNodes(Node localNode, Map nodes) {
        _log.info(DIndexNode.getNodeName(localNode)+" : "+DIndexNode.getNumIndexPartitions(localNode));

        Collection c=nodes.values();
        for (Iterator i=c.iterator(); i.hasNext(); ) {
            Node remoteNode=(Node)i.next();
            _log.info(DIndexNode.getNodeName(remoteNode)+" : "+DIndexNode.getNumIndexPartitions(remoteNode)+" - "+remoteNode);
        }
    }

    protected void execute(Plan plan, String correlationId, Quipu quipu) {
        quipu.increment(); // add a safety margin of '1', so if we are caught up by acks, waiting thread does not finish
        Iterator p=plan.getProducers().iterator();
        Iterator c=plan.getConsumers().iterator();

        int n=0;
        Pair consumer=null;
        while (p.hasNext()) {
            Pair producer=(Pair)p.next();
            while (producer._deviation>0) {
                if (consumer==null)
                    consumer=c.hasNext()?(Pair)c.next():null;
                    _log.info(""+(producer==null?-1:producer._deviation));
                    _log.info(""+(consumer==null?-1:consumer._deviation));
                    if (producer._deviation>=consumer._deviation) {
                        quipu.increment();
                        balance(producer._node, consumer._node, consumer._deviation, correlationId);
                        producer._deviation-=consumer._deviation;
                        consumer._deviation=0;
                        consumer=null;
                    } else {
                        quipu.increment();
                        balance(producer._node, consumer._node, producer._deviation, correlationId);
                        consumer._deviation-=producer._deviation;
                        producer._deviation=0;
                    }
            }
        }
        quipu.decrement(); // remove safety margin
    }

    public boolean balance(Node src, Node tgt, int amount, String correlationId) {
        /*if (src==_cluster.getLocalNode()) {
         // do something clever...
          } else*/ {
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
          return true;
    }

}
