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
    
    // cut-n-pasted from Rebalancer
    protected boolean contains(Node[] nodes, Node node) {
        for (int i=0; i<nodes.length; i++) {
            if (nodes[i].getDestination().equals(node.getDestination()))
                System.out.println("MATCH!");
                return true;
        }
        return false;
    }

    // should loop until it gets a successful outcome - emptying _flag each time...
    public void rebalanceClusterState() {
        //Collection excludedNodes=new ArrayList(_config.getLeavers()); // snapshot
        
        // snapshot leavers...
        Collection tmp=_config.getLeavers();
        Node [] leavers;
        synchronized (tmp) {
            leavers=(Node[])tmp.toArray(new Node[tmp.size()]);
        }
        
        int numParticipants=0;
        String correlationId;

        _remoteNodes=_config.getRemoteNodes(); // snapshot this at last possible moment...
        Plan plan=null;
//        if (excludedNodes.size()>0) {
//            // a node wants to leave - evacuate it
//            Node leaver=(Node)excludedNodes.iterator().next(); // FIXME - should be leavers...
//            plan=new EvacuationPlan(leaver, _localNode, _remoteNodes, _numItems);
//            numParticipants=_remoteNodes.length+1; // TODO - I think
//            correlationId=_localNode.getName()+"-rebalance-"+System.currentTimeMillis();
//        } else
//        {
            // standard rebalance...
            plan=new RebalancingPlan(_localNode, _remoteNodes, leavers, _numItems);
            numParticipants=_remoteNodes.length+1;
            correlationId=_localNode.getName()+"-leaving-"+System.currentTimeMillis();
//        }

        Map rvMap=_config.getRendezVousMap();
        Quipu rv=new Quipu(numParticipants-1);
        _log.info("SETTING UP RV FOR "+numParticipants+" PARTICIPANTS");
        rvMap.put(correlationId, rv);

        execute(plan, correlationId);

        boolean success=false;
        try {
            _log.info("WAITING ON RENDEZVOUS");
            rv.waitFor(5000); // unify with cluster heartbeat or parameterise - TODO
            _log.info("RENDEZVOUS SUCCESSFUL");
            Collection results=rv.getResults();
            success=true;
        } catch (TimeoutException e) {
            _log.warn("timed out waiting for response", e);
        } catch (InterruptedException e) {
            _log.warn("unexpected interruption", e);
        } finally {
            rvMap.remove(correlationId);
            // somehow check all returned success..

            // send EvacuationResponses to each leaving node... - hmmm....
            for (int i=0; i<_remoteNodes.length; i++) {
                Node node=_remoteNodes[i];
                if (contains(leavers, node)) {
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
                        _log.error("problem sending EvacuationResponse", e);
                    }
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

    protected void execute(Plan plan, String correlationId) {
        Iterator p=plan.getProducers().iterator();
        Iterator c=plan.getConsumers().iterator();

        Pair consumer=null;
        while (p.hasNext()) {
            Pair producer=(Pair)p.next();
            while (producer._deviation>0) {
                if (consumer==null)
                    consumer=c.hasNext()?(Pair)c.next():null;
                    _log.info(""+(producer==null?-1:producer._deviation));
                    _log.info(""+(consumer==null?-1:consumer._deviation));
                    if (producer._deviation>=consumer._deviation) {
                        balance(producer._node, consumer._node, consumer._deviation, correlationId);
                        producer._deviation-=consumer._deviation;
                        consumer._deviation=0;
                        consumer=null;
                    } else {
                        balance(producer._node, consumer._node, producer._deviation, correlationId);
                        consumer._deviation-=producer._deviation;
                        producer._deviation=0;
                    }
            }
        }
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
