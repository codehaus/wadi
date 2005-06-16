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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.activecluster.Cluster;
import org.activecluster.Node;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.wadi.impl.Quipu;

import EDU.oswego.cs.dl.util.concurrent.Slot;
import EDU.oswego.cs.dl.util.concurrent.TimeoutException;

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
    
    public void rebalanceClusterState() {
        int failures=0;
        try {
            
            Map nodeMap=_cluster.getNodes();
            
            Collection livingNodes=nodeMap.values();
            synchronized (livingNodes) {livingNodes=new ArrayList(livingNodes);} // snapshot
            livingNodes.add(_cluster.getLocalNode());
            
            
            Collection l=_config.getLeavers();
            synchronized (l) {l=new ArrayList(l);} // snapshot
            
            Collection leavingNodes=new ArrayList();
            for (Iterator i=l.iterator(); i.hasNext(); ) {
                Destination d=(Destination)i.next();
                Node leaver=(Node)nodeMap.get(d);
                if (leaver!=null) {
                    leavingNodes.add(leaver);
                    livingNodes.remove(leaver);
                }
            }
            
            Node [] living=(Node[])livingNodes.toArray(new Node[livingNodes.size()]);
            Node [] leaving=(Node[])leavingNodes.toArray(new Node[leavingNodes.size()]);
            
            String correlationId=_localNode.getName()+"-balancing-"+System.currentTimeMillis();
            RedistributionPlan plan=new RedistributionPlan(living, leaving, _numItems);
            
            printNodes(living, leaving);
            
            Map rvMap=_config.getRendezVousMap();
            Quipu rv=new Quipu(0);
            rvMap.put(correlationId, rv);
            execute(plan, correlationId, rv); // quipu will be incremented as participants are invited
            
            try {
                _log.info("WAITING ON RENDEZVOUS");
                if (rv.waitFor(_heartbeat)) {
                    _log.info("RENDEZVOUS SUCCESSFUL");
                    Collection results=rv.getResults();
                } else {
                    _log.info("RENDEZVOUS FAILED");
                    failures++;
                }
            } catch (TimeoutException e) {
                _log.warn("timed out waiting for response", e);
                failures++;
            } catch (InterruptedException e) {
                _log.warn("unexpected interruption", e);
                failures++;
            } finally {
                rvMap.remove(correlationId);
                // somehow check all returned success.. - TODO
                
                // send EvacuationResponses to each leaving node... - hmmm....
                Collection left=_config.getLeft();
                for (int i=0; i<leaving.length; i++) {
                    Node node=leaving[i];
                    if (!left.contains(node.getDestination())) {
                        _log.info("acknowledging evacuation of "+DIndex.getNodeName(node));
                        BucketEvacuationResponse response=new BucketEvacuationResponse();
                        try {
                            ObjectMessage om=_cluster.createObjectMessage();
                            om.setJMSReplyTo(_cluster.getLocalNode().getDestination());
                            om.setJMSDestination(node.getDestination());
                            om.setJMSCorrelationID(node.getName());
                            om.setObject(response);
                            _cluster.send(node.getDestination(), om);
                        } catch (JMSException e) {
                            _log.error("problem sending EvacuationResponse to "+DIndex.getNodeName(node), e);
                            failures++;
                        }
                        left.add(node.getDestination());
                    }
                }
            }
            printNodes(living, leaving);
        } catch (Throwable t) {
            _log.warn("problem rebalancing indeces", t);
            failures++;
        }
        
        if (failures>0)
            queueRebalancing();
    }
    
    protected void printNodes(Node[] living, Node[] leaving) {
        int total=0;
        for (int i=0; i<living.length; i++)
            total+=printNode(living[i]);
        for (int i=0; i<leaving.length; i++)
            total+=printNode(leaving[i]);
        _log.info("total : "+total);
    }
    
    protected int printNode(Node node) {
        if (node!=_cluster.getLocalNode())
            node=(Node)_cluster.getNodes().get(node.getDestination());
        if (node==null) {
            _log.info(DIndex.getNodeName(node)+" : <unknown>");
            return 0;
        } else {
            BucketKeys keys=DIndex.getBucketKeys(node);
            int amount=keys.size();
            _log.info(DIndex.getNodeName(node)+" : "+amount+" - "+keys);
            return amount;
        }
    }
    
    protected void execute(RedistributionPlan plan, String correlationId, Quipu quipu) {
        quipu.increment(); // add a safety margin of '1', so if we are caught up by acks, waiting thread does not finish
        Iterator p=plan.getProducers().iterator();
        Iterator c=plan.getConsumers().iterator();
        
        int n=0;
        BucketOwner consumer=null;
        while (p.hasNext()) {
            BucketOwner producer=(BucketOwner)p.next();
            Collection transfers=new ArrayList();
            while (producer._deviation>0) {
                if (consumer==null)
                    consumer=c.hasNext()?(BucketOwner)c.next():null;
                    if (producer._deviation>=consumer._deviation) {
                        transfers.add(new BucketTransfer(consumer._node.getDestination(), consumer._deviation));
                        producer._deviation-=consumer._deviation;
                        consumer._deviation=0;
                        consumer=null;
                    } else {
                        transfers.add(new BucketTransfer(consumer._node.getDestination(), producer._deviation));
                        consumer._deviation-=producer._deviation;
                        producer._deviation=0;
                    }
            }
            
            BucketTransferCommand command=new BucketTransferCommand((BucketTransfer[])transfers.toArray(new BucketTransfer[transfers.size()]));
            quipu.increment();
            try {
                ObjectMessage om=_cluster.createObjectMessage();
                om.setJMSReplyTo(_cluster.getLocalNode().getDestination());
                om.setJMSDestination(producer._node.getDestination());
                om.setJMSCorrelationID(correlationId);
                om.setObject(command);
                _cluster.send(producer._node.getDestination(), om);
            } catch (JMSException e) {
                _log.error("problem sending transfer command", e);
            }
        }
        quipu.decrement(); // remove safety margin
    }
    
}
